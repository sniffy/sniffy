# Sniffy Development

## Incubator for standalone projects

Certain functionality in Sniffy **SHOULD** be extracted to separate projects with dedicated source repository, artifacts and release cycle.
Not only this functionality can be used elsewhere, but it would also simplify contribution to it and streamline Sniffy development.

Main candidates for extraction:
1. Unsafe based reflection framework.
2. Framework for building pure-Java network interceptors.

## Exception Handling

Exception handling in Sniffy is based on next principles:
1. Sniffy shouldn't break existing application functionality in production:
   1. Any errors triggered by Sniffy in runtime **MUST** be caught and logged.
   2. Sniffy **SHOULD** not generate excessive logs, especially on network hot path.
   3. Any errors from wrapped object (Filter Chain, JDBC Objects, Sockets, etc.) **MUST** be rethrown as is.
   4. When running in tests (and especially during Sniffy own tests) any internal errors **MUST** be flagged immediately.
2. Sniffy uses Java `assert` functionality to validate assumptions about JDK core classes (like Sockets) and check internal Sniffy state during tests.
   `-esa` property enabled very useful assertions on internal JDK classes used by Sniffy (specifically in NIO stack).
3. In prod environment Sniffy uses `Prerequisite` based approach to validate assumptions before enabling certain functionality. Failure is logged but doesn't break the application. 
4. Sniffy uses `Unsafe.throwException` functionality to throw original exception.
5. Sniffy uses `Polyglog` for logging exception which also provides convenient "log once" functionality in addition to dependency-free facade over logging systems and facades.

## Static Code Analysis

Annotation from JSR305 (`@Nonnull` and `@Nullable` specifically) are used to facilitate static code analysis and achieve null-safety.
Another useful annotations are `@SuppressWarnings({"Convert2Diamond"})` which are placed on majority of production classes since target platfrom is Java 1.6 while IDE will most likely work in Java 1.8 source mode.

Adding static code analysis (for example for fighting NPE) is in the roadmap yet.

## Logging

TBD