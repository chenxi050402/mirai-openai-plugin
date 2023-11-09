package xyz.cssxsh.openai.chat

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import xyz.cssxsh.mirai.openai.config.ChatConfig
import xyz.cssxsh.openai.*

/**
 * [Chat](https://platform.openai.com/docs/api-reference/chat)
 * @since 1.2.0
 */
public class ChatController(private val client: OpenAiClient) {

    /**
     * [Create chat completion](https://platform.openai.com/docs/api-reference/chat/create)
     */
    public suspend fun create(request: ChatRequest, paid: Boolean): ChatInfo {
        var httpclient = client.http
        var apiurltemp = ChatConfig.APIURL
        if (paid) {
            httpclient = client.httppaid
            apiurltemp = ChatConfig.APIURLPaid
        }
        val response = httpclient.post("$apiurltemp/v1/chat/completions") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        return response.body()
    }

    /**
     * [Create completion](https://platform.openai.com/docs/api-reference/chat/create)
     */
    public suspend fun create(model: String, paid: Boolean = false, block: ChatRequest.Builder.() -> Unit): ChatInfo {
        return create(request = ChatRequest.Builder(model = model).apply(block).build(), paid)
    }
}