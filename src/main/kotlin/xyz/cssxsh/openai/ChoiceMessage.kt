package xyz.cssxsh.openai

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.json.*

/**
 * @param role either “system”, “user”, or “assistant”
 */

@Serializable
public data class ChoiceMessage(
    @SerialName("role")
    val role: String,
    @SerialName("content")
    val content: List<ChoiceMessagePart>
)

@Serializable
public data class ChoiceMessageText(
    @SerialName("role")
    val role: String,
    @SerialName("content")
    val content: String
)

@Serializable
public data class ChoiceMessagePart(
    @SerialName("type")
    val type: String,
    @SerialName("text")
    val text: String? = null,
    @SerialName("image_url")
    val imageUrl: ImageUrl? = null
) {
    // Optional constructor for 'text' type
    public constructor(text: String) : this("text", text)

    // Optional constructor for 'image_url' type
    public constructor(imageUrl: ImageUrl) : this("image_url", imageUrl = imageUrl)
}

@Serializable
public data class ImageUrl(
    @SerialName("url")
    val url: String
)