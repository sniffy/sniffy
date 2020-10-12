import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.string.shouldContain
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.request.*
import io.sniffy.Sniffy
import io.sniffy.configuration.SniffyConfiguration
import io.sniffy.registry.ConnectionsRegistry
import java.net.ConnectException

class KotestUsageTests : StringSpec({

    @Suppress("BlockingMethodInNonBlockingContext")
    "Ktor HTTP Client should be intercepted by Sniffy " {

        val client = HttpClient(Apache)

        SniffyConfiguration.INSTANCE.isMonitorNio = true
        Sniffy.initialize()
        ConnectionsRegistry.INSTANCE.setSocketAddressStatus("en.wikipedia.org", 443, -1)

        val exception = shouldThrow<ConnectException> {
            client.get("https://en.wikipedia.org/wiki/Main_Page")
        }
        exception.message shouldContain "refused by Sniffy"

    }

})