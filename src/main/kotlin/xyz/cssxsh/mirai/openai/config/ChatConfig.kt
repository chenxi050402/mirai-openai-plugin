package xyz.cssxsh.mirai.openai.config

import net.mamoe.mirai.console.data.*
import xyz.cssxsh.mirai.openai.config.CompletionConfig.provideDelegate
import xyz.cssxsh.openai.chat.*
import xyz.cssxsh.openai.completion.*

@PublishedApi
internal object ChatConfig : ReadOnlyPluginConfig("chat") {

    @ValueName("timeout")
    @ValueDescription("等待停止时间")
    val timeout: Long by value(60_000L)

    @ValueName("gpt_model")
    @ValueDescription("GPT Model")
    val model: String by value("gpt-3.5-turbo")

    @ValueName("api_url")
    @ValueDescription("API URL")
    val APIURL: String by value("https://api.openai.com")

    @ValueName("api_domain")
    @ValueDescription("API Domain")
    val APIDomain: String by value("api.openai.com")

    @ValueName("api_url_paid")
    @ValueDescription("API URL (Paid)")
    val APIURLPaid: String by value("https://api.openai.com")

    @ValueName("api_domain_paid")
    @ValueDescription("API Domain (Paid)")
    val APIDomainPaid: String by value("api.openai.com")

    @ValueName("max_tokens")
    @ValueDescription("Maximum length")
    val maxTokens: Int by value(512)

    @ValueName("temperature")
    @ValueDescription("Temperature")
    val temperature: Double by value(0.9)

    @ValueName("top_p")
    @ValueDescription("Top P")
    val topP: Double by value(1.0)

    @ValueName("presence_penalty")
    @ValueDescription("Presence Penalty")
    val presencePenalty: Double by value(0.6)

    @ValueName("frequency_penalty")
    @ValueDescription("Frequency Penalty")
    val frequencyPenalty: Double by value(0.0)

    @ValueName("id_to_person")
    @ValueDescription("Map QQ ID To Person")
    val personIDs: List<List<String>> by value()

    @ValueName("model_to_person")
    @ValueDescription("Map Chat Model To Person")
    val customModels: List<List<String>> by value()

    @ValueName("system_prompt")
    @ValueDescription("人设")
    val systemPrompt: String by value("You are a helpful assistant.")

    @ValueName("start_chat_msg")
    @ValueDescription("开始聊天提示")
    val startChatMsg: String by value("聊天将开始")

    @ValueName("end_chat_msg")
    @ValueDescription("停止聊天提示")
    val endChatMsg: String by value("聊天已终止")

    fun push(builder: ChatRequest.Builder) {
        builder.model = model
        builder.maxTokens = maxTokens
        builder.temperature = temperature
        builder.topP = topP
        builder.presencePenalty = presencePenalty
        builder.frequencyPenalty = frequencyPenalty
    }

    fun pushCustom(builder: ChatRequest.Builder, cModel: String) {
        builder.model = cModel
        builder.maxTokens = maxTokens
        builder.temperature = temperature
        builder.topP = topP
        builder.presencePenalty = presencePenalty
        builder.frequencyPenalty = frequencyPenalty
    }
}