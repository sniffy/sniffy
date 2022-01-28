package io.sniffy.test.kotest.usage

import io.kotest.core.extensions.TestCaseExtension
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import io.sniffy.Sniffy
import io.sniffy.SniffyAssertionError
import io.sniffy.Spy
import io.sniffy.Threads
import io.sniffy.configuration.SniffyConfiguration
import io.sniffy.socket.TcpConnections
import io.sniffy.sql.SqlQueries

/**
 * @since 3.1.7
 */
open class SniffyExtension(expectation: Spy.Expectation? = null) : TestCaseExtension {

    private val spy: Spy<*> = Sniffy.spy<Spy<*>>()

    init {
        expectation.let {
            spy.expect(it)
        }

        SniffyConfiguration.INSTANCE.isMonitorJdbc = true
        SniffyConfiguration.INSTANCE.isMonitorSocket = true
        SniffyConfiguration.INSTANCE.isMonitorNio = true
        Sniffy.initialize()
    }

    fun expect(expectation: Spy.Expectation): SniffyExtension {
        spy.expect(expectation)
        return this
    }

    override suspend fun intercept(testCase: TestCase, execute: suspend (TestCase) -> TestResult): TestResult {
        val testResult = execute.invoke(testCase)
        try {
            spy.verify()
        } catch (e: SniffyAssertionError) {
            return TestResult.Failure(testResult.duration, e)
        }
        return testResult
    }
}

class NoSocketsAllowedExtension(threads: Threads = Threads.ANY) : SniffyExtension(TcpConnections.none().threads(threads))

class NoSqlExtension(threads: Threads = Threads.ANY) : SniffyExtension(SqlQueries.noneQueries().threads(threads))

