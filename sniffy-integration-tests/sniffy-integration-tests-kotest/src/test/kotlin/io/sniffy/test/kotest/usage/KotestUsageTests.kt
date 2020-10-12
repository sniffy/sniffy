import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.request.*
import io.sniffy.registry.ConnectionsRegistry
import io.sniffy.socket.SnifferSocketImplFactory

class KotestUsageTests : StringSpec({

    "Ktor HTTP Client should be intercepted by Sniffy " {

        val client = HttpClient(Apache)

        SnifferSocketImplFactory.install()
        ConnectionsRegistry.INSTANCE.setSocketAddressStatus("en.wikipedia.org", 443, -1)

        val content: String = client.get("https://en.wikipedia.org/wiki/Main_Page")
        content.length shouldBeGreaterThan 0

    }

})