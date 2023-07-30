package xyz.cssxsh.openai.model

import io.ktor.client.call.*
import io.ktor.client.request.*
import xyz.cssxsh.mirai.openai.config.ChatConfig
import xyz.cssxsh.openai.*

/**
 * [Models](https://platform.openai.com/docs/api-reference/models)
 */
public class ModelController(private val client: OpenAiClient) {

    /**
     * [List models](https://platform.openai.com/docs/api-reference/models/list)
     */
    public suspend fun list(): List<ModelInfo> {
        val response = client.http.get(ChatConfig.APIURL + "/v1/models")
        val body = response.body<ListWrapper<ModelInfo>>()

        return body.data
    }

    /**
     * [Retrieve model](https://platform.openai.com/docs/api-reference/models/retrieve)
     */
    public suspend fun retrieve(model: String): ModelInfo {
        val response = client.http.get(ChatConfig.APIURL + "/v1/models/$model")

        return response.body()
    }

    /**
     * [Delete fine-tune model](https://platform.openai.com/docs/api-reference/fine-tunes/delete-model)
     */
    public suspend fun cancel(model: String): ModelInfo {
        val response = client.http.delete(ChatConfig.APIURL + "/v1/models/$model")

        return response.body()
    }
}