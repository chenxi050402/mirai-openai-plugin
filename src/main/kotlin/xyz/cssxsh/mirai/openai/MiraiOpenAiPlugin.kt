package xyz.cssxsh.mirai.openai

import kotlinx.coroutines.*
import net.mamoe.mirai.console.*
import net.mamoe.mirai.console.data.*
import net.mamoe.mirai.console.plugin.*
import net.mamoe.mirai.console.plugin.jvm.*
import net.mamoe.mirai.console.util.*
import net.mamoe.mirai.event.*
import net.mamoe.mirai.utils.*
import xyz.cssxsh.mirai.openai.config.*

/**
 * mirai-openai-plugin 插件主类
 */
public object MiraiOpenAiPlugin : KotlinPlugin(
    JvmPluginDescription(
        id = "xyz.cssxsh.mirai.plugin.mirai-openai-plugin",
        name = "mirai-openai-plugin",
        version = "1.2.3",
    ) {
        author("cssxsh")
    }
) {

    @PublishedApi
    internal val config: List<PluginConfig> by spi()

    @PublishedApi
    internal val listeners: List<ListenerHost> by spi()

    @Suppress("INVISIBLE_MEMBER")
    private inline fun <reified T : Any> spi(): Lazy<List<T>> = lazy {
        with(net.mamoe.mirai.console.internal.util.PluginServiceHelper) {
            jvmPluginClasspath.pluginClassLoader
                .findServices<T>()
                .loadAllServices()
        }
    }

    override fun onEnable() {
        // XXX: mirai console version check
        check(SemVersion.parseRangeRequirement(">= 2.12.0-RC").test(MiraiConsole.version)) {
            "$name $version 需要 Mirai-Console 版本 >= 2.12.0，目前版本是 ${MiraiConsole.version}"
        }

        for (config in config) config.reload()

        if (MiraiOpenAiConfig.token.isEmpty()) {
            val token = runBlocking { ConsoleInput.requestInput(hint = "请输入 OpenAI Secret Key") }

            @OptIn(ConsoleExperimentalApi::class)
            @Suppress("UNCHECKED_CAST")
            val value = MiraiOpenAiConfig.findBackingFieldValue<String>("token") as Value<String>
            value.value = token
        }
        if (MiraiOpenAiConfig.folder == "run") {
            @OptIn(ConsoleExperimentalApi::class)
            @Suppress("UNCHECKED_CAST")
            val value = MiraiOpenAiConfig.findBackingFieldValue<String>("image_folder") as Value<String>
            val folder = resolveDataFile("image")
            folder.mkdirs()
            value.value = folder.absolutePath
        }
        if (MiraiOpenAiConfig.proxy.isNotEmpty()) {
            logger.info { "代理已配置: ${MiraiOpenAiConfig.proxy}" }
        }

        for (config in config) config.save()

        for (listener in listeners) (listener as SimpleListenerHost).registerTo(globalEventChannel())

        if (MiraiOpenAiConfig.permission) {
            logger.info { "权限检查已开启" }
            MiraiOpenAiListener.completion
            MiraiOpenAiListener.image
            MiraiOpenAiListener.chat
            MiraiOpenAiListener.question
            MiraiOpenAiListener.reload
        } else {
            logger.info { "权限检查未开启" }
        }

        if (MiraiOpenAiConfig.chatByAt) {
            logger.warning { "@Bot 触发聊天已开启, 手机端引用消息会自带@，请注意不要误触" }
        }
    }

    override fun onDisable() {
        for (listener in listeners) (listener as SimpleListenerHost).cancel()
    }
}