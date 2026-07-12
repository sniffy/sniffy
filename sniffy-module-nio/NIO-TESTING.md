# NIO testing policy

NIO tests interact with both ordinary restorable state and JVM-global JDK infrastructure. They must not treat those
categories as interchangeable.

> Functional tests must not mutate JVM-global infrastructure. Tests whose purpose is to mutate global infrastructure
> must run in an isolated JVM fork.

## Locally restorable state

Configuration flags, `ConnectionsRegistry` rules and thread-local mode, thread-local discovered-address maps,
ClientHello cache entries, test hooks, spies, and listeners may be changed within one functional test. The test must
snapshot the exact prior value and restore it in `finally` or through `NioTestStateScope`/`NioTestStateRule`. Cleanup
runs in reverse order, continues after failures, and suppresses later cleanup failures onto the first failure.

## JVM-global infrastructure

The JDK `SelectorProvider` slot, provider installation/access resolvers, `Sniffy.nioModuleLoaded`, and module
initialization/reinitialization state belong to the entire JVM. A monitor, class ordering, retries, or method-level
teardown cannot isolate them.

`SniffySelectorProviderInstallationTest` therefore runs alone in the `nio-provider-lifecycle-execution` Surefire
execution with a new non-reused fork. The `NioProviderLifecycleTest` JUnit category enforces the same boundary when a
developer uses `-Dtest`, whose command-line selector otherwise overrides normal Surefire include/exclude patterns.
The lifecycle test snapshots and restores the original provider as a second line of protection.

The ordinary JUnit execution excludes that class. `NioFunctionalTestRunListener` bootstraps
`NioFunctionalTestEnvironment`, which installs one Sniffy provider for the functional fork and never uninstalls it.
Before every test the listener verifies the exact installed provider object and reports the expected/actual class,
last installation result, current thread, and JUnit test description if ownership changed.

Functional fixtures requiring raw JDK channels must call `NioFunctionalTestEnvironment.originalProvider()` and
construct the raw delegate locally. They must never uninstall the global provider. Calls to `SocketChannel.open()`,
`ServerSocketChannel.open()`, `Selector.open()`, and `Pipe.open()` exercise the globally installed Sniffy provider.

The functional suite runs alphabetically by default and is also verified in reverse-alphabetical order. Test state
must be independent of either order.
