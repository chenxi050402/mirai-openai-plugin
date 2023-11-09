package xyz.cssxsh.openai

import kotlinx.coroutines.*
import org.junit.jupiter.api.*
import xyz.cssxsh.openai.image.*

internal class OpenAiClientTest {
    private val config = object : OpenAiClientConfig {
        override val proxy: String = ""
        override val doh: String = "https://public.dns.iij.jp/dns-query"
        override val ipv6: Boolean = true
        override val timeout: Long = 30_000L
        override val token: String = System.getenv("OPENAI_TOKEN")
        override val tokenpaid: String = System.getenv("OPENAI_TOKEN")

    }
    private val client = OpenAiClient(config = config)

    @Test
    fun models(): Unit = runBlocking {
        val models = client.model.list()
        val random = models.random()
        val model = client.model.retrieve(model = random.id)

        Assertions.assertEquals(random.id, model.id)
        Assertions.assertEquals(random, model)
    }

    @Test
    fun completions(): Unit = runBlocking {
        val model = "text-davinci-003"
        val completion = client.completion.create(model) {
            prompt("你好")
            maxTokens(256)
        }

        Assertions.assertEquals(model, completion.model)
        Assertions.assertFalse(completion.choices.isEmpty())
    }

    @Test
    fun images(): Unit = runBlocking {
        val prompt = "风景图"
        val image = client.image.create(prompt) {
            format = ImageResponseFormat.URL
        }

        Assertions.assertFalse(image.data.isEmpty())
        Assertions.assertTrue(image.data.first().url.isNotEmpty())
    }

    @Test
    fun files(): Unit = runBlocking {
        val files = client.file.list()
    }
}