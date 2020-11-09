package io.sniffy.test.kotest.usage

import io.kotest.core.extensions.TestCaseExtension
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import io.sniffy.Sniffy
import io.sniffy.configuration.SniffyConfiguration
import io.sniffy.registry.ConnectionsRegistry

class DisableSocketsExtension : TestCaseExtension {

    init {
        SniffyConfiguration.INSTANCE.isMonitorSocket = true
        SniffyConfiguration.INSTANCE.isMonitorNio = true
        Sniffy.initialize()
    }

    override suspend fun intercept(testCase: TestCase, execute: suspend (TestCase) -> TestResult): TestResult {
        try {
            ConnectionsRegistry.INSTANCE.setSocketAddressStatus(null, null, -1)
            return execute.invoke(testCase)
        } finally {
            ConnectionsRegistry.INSTANCE.clear()
        }
    }

}