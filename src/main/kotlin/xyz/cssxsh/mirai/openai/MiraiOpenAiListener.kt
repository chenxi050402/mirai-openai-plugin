package xyz.cssxsh.mirai.openai

import io.ktor.client.call.*
import io.ktor.client.network.sockets.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlinx.serialization.SerialName
import net.mamoe.mirai.console.command.CommandSender.Companion.toCommandSender
import net.mamoe.mirai.console.permission.*
import net.mamoe.mirai.console.permission.PermissionService.Companion.hasPermission
import net.mamoe.mirai.contact.Contact.Companion.uploadImage
import net.mamoe.mirai.event.*
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.message.*
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import net.mamoe.mirai.message.data.MessageChain.Companion.serializeToJsonString
import net.mamoe.mirai.message.data.MessageSource.Key.quote
import net.mamoe.mirai.utils.*
import xyz.cssxsh.mirai.openai.config.*
import xyz.cssxsh.openai.*
import xyz.cssxsh.openai.image.*
import java.io.File
import java.util.Base64
import java.util.UUID
import kotlin.coroutines.*
import kotlin.reflect.typeOf

@PublishedApi
internal object MiraiOpenAiListener : SimpleListenerHost() {
    private val client = OpenAiClient(config = MiraiOpenAiConfig)
    private val folder = File(MiraiOpenAiConfig.folder)
    public val logger = MiraiLogger.Factory.create(this::class)
    private val lock: MutableMap<Long, MessageEvent> = java.util.concurrent.ConcurrentHashMap()
    internal val completion: Permission by MiraiOpenAiPermissions
    internal val image: Permission by MiraiOpenAiPermissions
    internal val chat: Permission by MiraiOpenAiPermissions
    internal val question: Permission by MiraiOpenAiPermissions
    internal val reload: Permission by MiraiOpenAiPermissions

    override fun handleException(context: CoroutineContext, exception: Throwable) {
        when (exception) {
            is kotlinx.coroutines.CancellationException -> {
                // ...
            }
            is ExceptionInEventHandlerException -> {
                logger.warning({ "MiraiOpenAiListener with ${exception.event}" }, exception.cause)
                if (MiraiOpenAiConfig.reply && exception.event is MessageEvent) launch {
                    val event = exception.event as MessageEvent
                    when (val cause = exception.cause) {
                        is SocketTimeoutException, is ConnectTimeoutException -> {
                            event.subject.sendMessage(event.message.quote() + "OpenAI API 超时 请重试")
                        }
                        is OverFileSizeMaxException -> {
                            event.subject.sendMessage(event.message.quote() + "OpenAI API 生成图片过大 请重试")
                        }
                        is IllegalStateException -> {
                            val info = cause.message
                            when {
                                info == null -> Unit
                                info.endsWith(", failed on all servers.") -> {
                                    event.subject.sendMessage(event.message.quote() + "OpenAI API 生成图片上传失败 请重试")
                                }
                            }
                        }
                        is NoTransformationFoundException -> {
                            val info = cause.message
                            when {
                                info == null -> Unit
                                "cloudflare" in info -> {
                                    event.subject.sendMessage(event.message.quote() + "OpenAI API 访问失败 请检查代理设置")
                                }
                            }
                        }
                    }
                }
            }
            else -> Unit
        }
    }

    @EventHandler
    suspend fun MessageEvent.handle() {
        if (sender.id in lock) return
        val content = message.contentToString()
        val message: Message = when {
            content.startsWith(MiraiOpenAiConfig.completion)
                && (MiraiOpenAiConfig.permission.not() || toCommandSender().hasPermission(completion))
            -> if (lock.size >= MiraiOpenAiConfig.limit) {
                "聊天服务已开启过多，请稍后重试".toPlainText()
            } else {
                completion(event = this)
            }
            content.startsWith(MiraiOpenAiConfig.image)
                && (MiraiOpenAiConfig.permission.not() || toCommandSender().hasPermission(image))
            -> if (lock.size >= MiraiOpenAiConfig.limit) {
                "聊天服务已开启过多，请稍后重试".toPlainText()
            } else {
                image(event = this)
            }
            content.startsWith(MiraiOpenAiConfig.chat)
                && (MiraiOpenAiConfig.permission.not() || toCommandSender().hasPermission(chat))
            -> if (lock.size >= MiraiOpenAiConfig.limit) {
                "聊天服务已开启过多，请稍后重试".toPlainText()
            } else {
                chat(event = this)
            }
            content.startsWith("!" + MiraiOpenAiConfig.chat)
                    && (MiraiOpenAiConfig.permission.not() || toCommandSender().hasPermission(chat))
            -> if (lock.size >= MiraiOpenAiConfig.limit) {
                "聊天服务已开启过多，请稍后重试".toPlainText()
            } else {
                chat(event = this)
            }
            content.startsWith(MiraiOpenAiConfig.question)
                && (MiraiOpenAiConfig.permission.not() || toCommandSender().hasPermission(question))
            -> if (lock.size >= MiraiOpenAiConfig.limit) {
                "聊天服务已开启过多，请稍后重试".toPlainText()
            } else {
                question(event = this)
            }
            content.startsWith(MiraiOpenAiConfig.reload)
                && (MiraiOpenAiConfig.permission.not() || toCommandSender().hasPermission(reload))
            -> with(MiraiOpenAiPlugin) {
                config.forEach { config ->
                    config.reload()
                }
                client.clearToken()
                "OPENAI 配置已重载".toPlainText()
            }
            message.findIsInstance<At>()?.target == bot.id && MiraiOpenAiConfig.chatByAt
                && (MiraiOpenAiConfig.permission.not() || toCommandSender().hasPermission(chat))
            -> if (lock.size >= MiraiOpenAiConfig.limit) {
                "聊天服务已开启过多，请稍后重试".toPlainText()
            } else {
                chat(event = this)
            }
            else -> return
        }

        subject.sendMessage(message)
    }

    private suspend fun completion(event: MessageEvent): Message {
        val prompt = event.message.contentToString()
            .removePrefix(MiraiOpenAiConfig.completion)

        logger.verbose { "prompt: $prompt" }

        val completion = client.completion.create(model = "text-davinci-003") {
            prompt(prompt)
            user(event.senderName)
            CompletionConfig.push(this)
        }
        logger.debug { "${completion.model} - ${completion.usage}" }
        return buildMessageChain {
            add(event.message.quote())
            for (choice in completion.choices) {
                append("Choice.").append(choice.index.toString()).append(" FinishReason: ").append(choice.finishReason)
                if (choice.text.firstOrNull() != '\n') append('\n')
                append(choice.text).append('\n')
            }
        }
    }

    private suspend fun image(event: MessageEvent): Message {
        val prompt = event.message.contentToString()
            .removePrefix(MiraiOpenAiConfig.image)

        logger.verbose { "prompt: $prompt" }

        val result = client.image.create(prompt = prompt) {
            user(event.senderName)
            ImageConfig.push(this)
        }

        return buildMessageChain {
            add(event.message.quote())
            for (item in result.data) {
                val file = store(item = item, folder = folder)
                val image = event.subject.uploadImage(file)

                add(image)
            }
        }
    }

    private suspend fun chat(event: MessageEvent): Message {
        var initialMessage = ""
        if (!event.message.contentToString().startsWith("!")) initialMessage = event.message.contentToString().removePrefix(MiraiOpenAiConfig.chat)
        else initialMessage = event.message.contentToString().removePrefix("!").removePrefix(MiraiOpenAiConfig.chat)
        val preInitialMessage = "\\n\\nThe person you're chatting to told you: "
        var combinedInitialMessage = ""
        if (initialMessage.replace("\\s".toRegex(), "") != "" && !initialMessage.startsWith("?model=")) combinedInitialMessage = preInitialMessage + initialMessage

        for (id in ChatConfig.personIDs) {
            if (event.sender.id == id[0].toLong()) {
                combinedInitialMessage += "First message from user: I'm " + id[1] + "."
                break
            }
        }
        val system = listOf(ChoiceMessagePart(ChatConfig.systemPrompt + combinedInitialMessage))
        launch {
            lock[event.sender.id] = event
            val buffer = mutableListOf<ChoiceMessage>()
            buffer.add(ChoiceMessage(
                role = "system",
                content = system
            ))
            while (isActive) {
                val next = event.nextMessage(ChatConfig.timeout, EventPriority.HIGH, intercept = true)
                val content = mutableListOf<ChoiceMessagePart>()
                var addcontent = ""
                var custom = false
                var usemodel = ChatConfig.model
                if (!event.message.contentToString().startsWith("!")) {
                    if (initialMessage.startsWith("?model=")) {
                        usemodel = initialMessage.removePrefix("?model=")
                        custom = true
                    }
                    else {
                        for (id in ChatConfig.customModels) {
                            if (event.sender.id == id[0].toLong()) {
                                usemodel = id[1]
                                custom = true
                                break
                            }
                        }
                    }
                }
                for (i in next) {
                    if (i is Image && usemodel.contains("vision")) {
                        content.add(ChoiceMessagePart(type = "text", text = addcontent))
                        addcontent = ""
                        content.add(ChoiceMessagePart(imageUrl = ImageUrl(i.queryUrl())))
                    } else if (i is PlainText || i is Face || i is PokeMessage || i is VipFace || i is LightApp || i is MarketFace || i is Image || i is ForwardMessage || i is FileMessage || i is Audio) {
                        addcontent += i.contentToString()
                    }
                }
                if (addcontent != "") {
                    content.add(ChoiceMessagePart(type = "text", text = addcontent))
                }
                logger.info(content.toString())
                if (next.contentToString() == MiraiOpenAiConfig.stop) break
                var paid = false

                buffer.add(ChoiceMessage(
                    role = "user",
                    content = content
                ))

                for (id in ChatConfig.customModels) {
                    if (event.sender.id == id[0].toLong()) {
                        if (id[2] == "Paid") {
                            paid = true
                        }
                        break
                    }
                }

                val chat = client.chat.create(model = "gpt-3.5-turbo", paid = paid) {
                    messages(buffer)
                    user(event.senderName)
                    if (custom) ChatConfig.pushCustom(this, usemodel)
                    else ChatConfig.push(this)
                }
                logger.debug { "${chat.model} - ${chat.usage}" }
                val reply = chat.choices.first()
                val message = if (reply.message != null) {
                    ChoiceMessage(role = "assistant", content = listOf(ChoiceMessagePart(text = reply.message.content)))
                } else {
                    ChoiceMessage(
                        role = "assistant",
                        content = listOf(ChoiceMessagePart(reply.text))
                    )
                }
                buffer.add(message)
                if (chat.usage.totalTokens > ChatConfig.maxTokens * 0.96) {
                    buffer.removeFirstOrNull()
                }
                launch {
                    event.subject.sendMessage(next.quote() + message.content[0].text.toString())
                }
                when (reply.finishReason) {
                    "length" -> logger.warning { "max_tokens not enough for ${next.quote()} " }
                    else -> Unit
                }
            }
        }.invokeOnCompletion { cause ->
            lock.remove(event.sender.id)
            logger.debug { "聊天已终止" }
            if (cause != null) {
                val exception = ExceptionInEventHandlerException(event = event, cause = cause)
                handleException(coroutineContext, exception)
            }
            if (MiraiOpenAiConfig.bye) {
                launch {
                    event.subject.sendMessage(event.message.quote() + ChatConfig.endChatMsg)
                }
            }
        }

        return event.message.quote() + ChatConfig.startChatMsg
    }

    private suspend fun question(event: MessageEvent): Message {
        val prompt = event.message.contentToString()
            .removePrefix(MiraiOpenAiConfig.question)
        launch {
            lock[event.sender.id] = event
            val buffer = StringBuffer(prompt)
            buffer.append('\n')
            while (isActive) {
                val next = event.nextMessage(QuestionConfig.timeout, EventPriority.HIGH, intercept = true)
                val content = next.contentToString()
                if (content == MiraiOpenAiConfig.stop) break

                if (buffer.length > ChatConfig.maxTokens * 0.6) {
                    buffer.delete(prompt.length + 1, buffer.lastIndexOf("Human: "))
                }

                buffer.append("Q: ").append(content).append('\n')
                buffer.append("A: ")

                logger.verbose { "prompt: $buffer" }

                val completion = client.completion.create(model = "text-davinci-003") {
                    prompt(buffer.toString())
                    user(event.senderName)
                    QuestionConfig.push(this)
                    stop("\n")
                }
                logger.debug { "${completion.model} - ${completion.usage}" }
                val reply = completion.choices.first()
                buffer.append(reply.text).append('\n')
                launch {
                    event.subject.sendMessage(next.quote() + reply.text)
                }
                when (reply.finishReason) {
                    "length" -> logger.warning { "max_tokens not enough for ${next.quote()} " }
                    else -> Unit
                }
            }
        }.invokeOnCompletion { cause ->
            lock.remove(event.sender.id)
            logger.debug { "问答已终止" }
            if (cause != null) {
                val exception = ExceptionInEventHandlerException(event = event, cause = cause)
                handleException(coroutineContext, exception)
            }
            if (MiraiOpenAiConfig.bye) {
                launch {
                    event.subject.sendMessage(event.message.quote() + "问答已终止")
                }
            }
        }

        return event.message.quote() + "问答将开始"
    }

    private suspend fun store(item: ImageInfo.Data, folder: File): File {
        return when {
            item.url.isNotEmpty() -> {
                val response = client.http.get(item.url)
                val filename = response.headers[HttpHeaders.ContentDisposition]
                    ?.let { ContentDisposition.parse(it).parameter(ContentDisposition.Parameters.FileName) }
                    ?: "${UUID.randomUUID()}.${response.contentType()?.contentSubtype ?: "png"}"

                val target = folder.resolve(filename)
                response.bodyAsChannel().copyAndClose(target.writeChannel())
                // XXX: target.writeChannel() 不堵塞 https://github.com/cssxsh/mirai-openai-plugin/issues/2
                delay(3_000)
                target
            }
            item.base64.isNotEmpty() -> {
                val bytes = Base64.getDecoder().decode(item.base64)

                val target = folder.resolve("${UUID.randomUUID()}.png")
                target.writeBytes(bytes)

                target
            }
            else -> throw IllegalArgumentException("data is empty")
        }
    }
}