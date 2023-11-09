package xyz.cssxsh.openai

import kotlinx.serialization.*

@Serializable
public data class Choice(
    @SerialName("finish_reason")
    val finishReason: String? = null,
    @SerialName("finish_details")
    val finishDetails: ChoiceFinish? = null,
    @SerialName("index")
    val index: Int = 0,
    @SerialName("logprobs")
    val logprobs: Int? = null,
    @SerialName("text")
    val text: String = "",
    @SerialName("message")
    val message: ChoiceMessageText? = null
)

@Serializable
public data class ChoiceFinish (
    @SerialName("type")
    val type: String = "",
    @SerialName("stop")
    val stop: String = ""
)
