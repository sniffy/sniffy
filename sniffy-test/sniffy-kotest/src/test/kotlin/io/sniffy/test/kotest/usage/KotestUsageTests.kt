package io.sniffy.test.kotest.usage

import com.hazelcast.client.HazelcastClient
import com.hazelcast.client.config.*
import com.hazelcast.config.Config
import com.hazelcast.config.NetworkConfig
import com.hazelcast.core.Hazelcast
import com.hazelcast.core.HazelcastInstance
import io.kotest.assertions.fail
import io.kotest.core.extensions.TestCaseExtension
import io.kotest.core.spec.style.StringSpec
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import io.kotest.core.test.TestStatus
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.instanceOf
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.request.*
import io.sniffy.SniffyAssertionError

class ExpectSniffyAssertionExceptionExtension : TestCaseExtension {

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
    "Ktor HTTP Client should be intercepted by Sniffy".config(extensions = listOf( // <1>
            ExpectSniffyAssertionExceptionExtension(),
            NoSocketsAllowedExtension() // <2>
    )) {

        val client = HttpClient(Apache)

        client.get("https://en.wikipedia.org/wiki/Main_Page")

    }

    "Hazelcast client should be intercepted by Sniffy".config(
            extensions = listOf(DisableSocketsExtension())) {// <3>

        val serverConfig: Config = Config("my-hazelcast").apply {
            networkConfig = NetworkConfig().apply {
                port = 6000
                portCount = 1
            }
        }

        val hzInstance: HazelcastInstance = Hazelcast.newHazelcastInstance(serverConfig)
        hzInstance.getMap<Any, Any>("my-distributed-map").put("key", "value")

        try {
            val config = ClientConfig().apply {
                instanceName = "my-hazelcast"
                networkConfig = ClientNetworkConfig().apply {
                    addresses = listOf("localhost:6000")
                }
                connectionStrategyConfig = ClientConnectionStrategyConfig().apply {
                    connectionRetryConfig = ConnectionRetryConfig().apply {
                        initialBackoffMillis = 50
                        maxBackoffMillis = 100
                        clusterConnectTimeoutMillis = 200
                    }
                }
            }
            val failoverConfig = ClientFailoverConfig().apply {
                tryCount = 1
                clientConfigs = listOf(config)
            }
            val hazelcastClient = HazelcastClient.newHazelcastFailoverClient(failoverConfig)
            hazelcastClient.getMap<Any, Any>("my-distributed-map").get("key") shouldBe "value"
            fail("Should have been refused by Sniffy")
        } catch (e: Exception) {
            e shouldNotBe null
        } finally {
            hzInstance.shutdown()
        }

    }

})