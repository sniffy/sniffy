import io.kotest.core.extensions.TestCaseExtension
import io.kotest.core.spec.style.StringSpec
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import io.kotest.core.test.TestStatus
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.instanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.request.*
import io.sniffy.Sniffy
import io.sniffy.SniffyAssertionError
import io.sniffy.Spy
import io.sniffy.Threads
import io.sniffy.nio.SniffySelectorProvider
import io.sniffy.socket.TcpConnections

class NoSocketExtension(val threads: Threads = Threads.ANY) : TestCaseExtension {

    init {
        Sniffy.initialize()
        SniffySelectorProvider.install()
    }

    override suspend fun intercept(testCase: TestCase, execute: suspend (TestCase) -> TestResult): TestResult {
        val spy = Sniffy.spy<Spy<*>>()
        val testResult = execute.invoke(testCase)
        try {
            spy.verify(TcpConnections.none().threads(threads))
        } catch (e: SniffyAssertionError) {
            return testResult.copy(status = TestStatus.Failure, error = e)
        }
        return testResult
    }

}

class ExpectExceptionExtension : TestCaseExtension {

    override suspend fun intercept(testCase: TestCase, execute: suspend (TestCase) -> TestResult): TestResult {
        val testResult = execute(testCase)
        try {
            testResult.error shouldBe instanceOf(SniffyAssertionError::class)
        } catch (e: Exception) {
            return testResult.copy(status = TestStatus.Failure, error = e)
        }
        return TestResult.success(testResult.duration)
    }
}

class KotestUsageTests : StringSpec({

    @Suppress("BlockingMethodInNonBlockingContext")
    "Ktor HTTP Client should be intercepted by Sniffy"
            .config(extensions = listOf(ExpectExceptionExtension(), NoSocketExtension(Threads.ANY))) {

        val client = HttpClient(Apache)

        client.get("https://en.wikipedia.org/wiki/Main_Page")

        /*SniffyConfiguration.INSTANCE.isMonitorNio = true
        Sniffy.initialize()
        ConnectionsRegistry.INSTANCE.setSocketAddressStatus("en.wikipedia.org", 443, -1)

        val exception = shouldThrow<ConnectException> {
            client.get("https://en.wikipedia.org/wiki/Main_Page")
        }
        exception.message shouldContain "refused by Sniffy"*/

    }

})