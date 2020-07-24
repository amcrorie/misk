package misk.web.marshal

import com.squareup.moshi.Moshi
import misk.inject.KAbstractModule
import misk.testing.MiskTest
import misk.testing.MiskTestModule
import misk.web.Get
import misk.web.ConcurrencyLimitsOptOut
import misk.web.Response
import misk.web.ResponseContentType
import misk.web.WebActionModule
import misk.web.WebTestingModule
import misk.web.actions.WebAction
import misk.web.jetty.JettyService
import misk.web.mediatype.MediaTypes
import misk.web.toResponseBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.ByteString
import okio.ByteString.Companion.encodeUtf8
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import javax.inject.Inject

@MiskTest(startService = true)
internal class JsonResponseTest {
  data class Packet(val message: String)

  @MiskTestModule
  val module = TestModule()

  @Inject private lateinit var jettyService: JettyService
  @Inject private lateinit var moshi: Moshi
  private val packetJsonAdapter get() = moshi.adapter(Packet::class.java)

  @Test
  fun returnAsObject() {
    assertThat(get("/response/as-object").message).isEqualTo("as-object")
  }

  @Test
  fun returnAsString() {
    assertThat(get("/response/as-string").message).isEqualTo("as-string")
  }

  @Test
  fun returnAsByteString() {
    assertThat(get("/response/as-byte-string").message).isEqualTo("as-byte-string")
  }

  @Test
  fun returnAsResponseBody() {
    assertThat(get("/response/as-response-body").message).isEqualTo("as-response-body")
  }

  @Test
  fun returnAsObjectResponse() {
    assertThat(get("/response/as-wrapped-object").message).isEqualTo("as-object")
  }

  @Test
  fun returnAsStringResponse() {
    assertThat(get("/response/as-wrapped-string").message).isEqualTo("as-string")
  }

  @Test
  fun returnAsByteStringResponse() {
    assertThat(get("/response/as-wrapped-byte-string").message).isEqualTo("as-byte-string")
  }

  @Test
  fun returnAsResponseBodyResponse() {
    assertThat(get("/response/as-wrapped-response-body").message).isEqualTo("as-response-body")
  }

  class ReturnAsObject @Inject constructor() : WebAction {
    @Get("/response/as-object")
    @ConcurrencyLimitsOptOut // TODO: Remove after 2020-08-01 (or use @AvailableWhenDegraded).
    @ResponseContentType(MediaTypes.APPLICATION_JSON)
    fun call() = Packet("as-object")
  }

  class ReturnAsString @Inject constructor() : WebAction {
    @Get("/response/as-string")
    @ConcurrencyLimitsOptOut // TODO: Remove after 2020-08-01 (or use @AvailableWhenDegraded).
    @ResponseContentType(MediaTypes.APPLICATION_JSON)
    fun call() = "{\"message\":\"as-string\"}"
  }

  class ReturnAsByteString @Inject constructor() : WebAction {
    @Get("/response/as-byte-string")
    @ConcurrencyLimitsOptOut // TODO: Remove after 2020-08-01 (or use @AvailableWhenDegraded).
    @ResponseContentType(MediaTypes.APPLICATION_JSON)
    fun call(): ByteString = "{\"message\":\"as-byte-string\"}".encodeUtf8()
  }

  class ReturnAsResponseBody @Inject constructor() : WebAction {
    @Get("/response/as-response-body")
    @ConcurrencyLimitsOptOut // TODO: Remove after 2020-08-01 (or use @AvailableWhenDegraded).
    @ResponseContentType(MediaTypes.APPLICATION_JSON)
    fun call() = "{\"message\":\"as-response-body\"}".toResponseBody()
  }

  class ReturnAsObjectResponse @Inject constructor() : WebAction {
    @Get("/response/as-wrapped-object")
    @ConcurrencyLimitsOptOut // TODO: Remove after 2020-08-01 (or use @AvailableWhenDegraded).
    @ResponseContentType(MediaTypes.APPLICATION_JSON)
    fun call() = Response(Packet("as-object"))
  }

  class ReturnAsStringResponse @Inject constructor() : WebAction {
    @Get("/response/as-wrapped-string")
    @ConcurrencyLimitsOptOut // TODO: Remove after 2020-08-01 (or use @AvailableWhenDegraded).
    @ResponseContentType(MediaTypes.APPLICATION_JSON)
    fun call() = Response("{\"message\":\"as-string\"}")
  }

  class ReturnAsByteStringResponse @Inject constructor() : WebAction {
    @Get("/response/as-wrapped-byte-string")
    @ConcurrencyLimitsOptOut // TODO: Remove after 2020-08-01 (or use @AvailableWhenDegraded).
    @ResponseContentType(MediaTypes.APPLICATION_JSON)
    fun call() = Response("""{"message":"as-byte-string"}""".encodeUtf8())
  }

  class ReturnAsResponseBodyResponse @Inject constructor() : WebAction {
    @Get("/response/as-wrapped-response-body")
    @ConcurrencyLimitsOptOut // TODO: Remove after 2020-08-01 (or use @AvailableWhenDegraded).
    @ResponseContentType(MediaTypes.APPLICATION_JSON)
    fun call() = Response("""{"message":"as-response-body"}""".encodeUtf8())
  }

  class TestModule : KAbstractModule() {
    override fun configure() {
      install(WebTestingModule())
      install(WebActionModule.create<ReturnAsObject>())
      install(WebActionModule.create<ReturnAsString>())
      install(WebActionModule.create<ReturnAsByteString>())
      install(WebActionModule.create<ReturnAsResponseBody>())
      install(WebActionModule.create<ReturnAsObjectResponse>())
      install(WebActionModule.create<ReturnAsStringResponse>())
      install(WebActionModule.create<ReturnAsByteStringResponse>())
      install(WebActionModule.create<ReturnAsResponseBodyResponse>())
    }
  }

  private fun get(path: String): Packet = call(Request.Builder()
      .url(jettyService.httpServerUrl.newBuilder().encodedPath(path).build())
      .get())

  private fun call(request: Request.Builder): Packet {
    request.header("Accept", MediaTypes.APPLICATION_JSON)

    val httpClient = OkHttpClient()
    val response = httpClient.newCall(request.build()).execute()
    assertThat(response.code).isEqualTo(200)
    assertThat(response.header("Content-Type")).isEqualTo(MediaTypes.APPLICATION_JSON)
    return packetJsonAdapter.fromJson(response.body!!.source())!!
  }
}
