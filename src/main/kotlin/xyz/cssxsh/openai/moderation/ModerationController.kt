package xyz.cssxsh.openai.moderation

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import xyz.cssxsh.mirai.openai.config.ChatConfig
import xyz.cssxsh.openai.*

/**
 * [Moderations](https://platform.openai.com/docs/api-reference/moderations)
 */
public class ModerationController(private val client: OpenAiClient) {

    /**
     * [Create moderation](https://platform.openai.com/docs/api-reference/moderations/create)
     */
    public suspend fun create(request: ModerationRequest): ModerationResult {
        val response = client.http.post(ChatConfig.APIURL + "/v1/images/generations") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        return response.body()
    }

    /**
     * [Create moderation](https://platform.openai.com/docs/api-reference/moderations/create)
     */
    public suspend fun create(input: String, model: String = "text-moderation-latest"): ModerationResult {
        return create(request = ModerationRequest(input = input, model = model))
    }
}