package xyz.cssxsh.openai.edit

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import xyz.cssxsh.mirai.openai.config.ChatConfig
import xyz.cssxsh.openai.*

/**
 * [Edits](https://platform.openai.com/docs/api-reference/edits)
 */
public class EditController(private val client: OpenAiClient) {

    /**
     * [Create edit](https://platform.openai.com/docs/api-reference/edits/create)
     */
    public suspend fun create(request: EditRequest): EditInfo {
        val response = client.http.post(ChatConfig.APIURL + "/v1/edits") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        return response.body()
    }

    /**
     * [Create edit](https://platform.openai.com/docs/api-reference/edits/create)
     */
    public suspend fun create(model: String, block: EditRequest.Builder.() -> Unit): EditInfo {
        return create(request = EditRequest.Builder(model = model).apply(block).build())
    }
}