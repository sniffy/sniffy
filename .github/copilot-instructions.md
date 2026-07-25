# Sniffy - Java Profiler and Network Traffic Analyzer

Sniffy is a Java profiler which shows results directly in your browser. It also brings profiling to unit tests and allows disabling certain outgoing connections for fault-tolerance testing. You can also use Sniffy to capture traffic sent or received by your application - like Wireshark but without admin permission or 3rd-party dependencies.

**ALWAYS reference these instructions first and fallback to search or bash commands only when you encounter unexpected information that does not match the info here.**

`AGENTS.md` is the authoritative repository policy. In particular, follow its standard-tooling and infrastructure-
approval gate: prefer maintained tools and declarative configuration for common build, CI, release, dependency, and
security problems, and do not implement bespoke infrastructure or a materially new workflow/job without the required
maintainer-approved issue rationale.

## Working Effectively

**CRITICAL BUILD REQUIREMENTS:**
- **NEVER CANCEL builds or tests** - Builds may take 2+ minutes, tests may take additional time. Set timeouts to 120+ minutes for full builds.
- Use `mvn -Djacoco.version=0.8.8` to avoid SNAPSHOT dependency issues with JaCoCo plugin.
- The build system uses Maven 3.9+ with Java 17 and supports Java 8/11/17 compatibility.

### Bootstrap and Build Commands
Execute these commands in sequence to build the project:

```bash
# 1. Dependency resolution (2 minutes) - NEVER CANCEL, set timeout to 300+ seconds
mvn -T 1C -B de.qaware.maven:go-offline-maven-plugin:resolve-dependencies -U -Djacoco.version=0.8.8

# 2. Full build without tests (1 minute) - NEVER CANCEL, set timeout to 180+ seconds  
mvn -T 1C -B clean install -Djacoco.version=0.8.8 -Dgpg.skip=true -DskipTests=true

# 3. Full build with tests (2+ minutes) - NEVER CANCEL, set timeout to 300+ seconds
mvn -T 1C -B clean install -Djacoco.version=0.8.8 -Dgpg.skip=true -Dmaven.test.failure.ignore=true

# 4. Core module only build with tests (25 seconds) - for faster iteration
mvn -T 1C -B clean test -pl sniffy-core -Djacoco.version=0.8.8 -Dgpg.skip=true
```

**CRITICAL**: Always use `-Djacoco.version=0.8.8` to avoid SNAPSHOT dependency failures. The default JaCoCo version (0.8.9-SNAPSHOT) is not available and will cause build failures.

### Test Commands
```bash
# Run all tests allowing failures (2+ minutes) - NEVER CANCEL
mvn -T 1C -B test -Djacoco.version=0.8.8 -Dmaven.test.failure.ignore=true

# Run Spring Boot integration tests (13 seconds)
cd sniffy-integration-tests/sniffy-integration-tests-spring-boot
mvn test -Djacoco.version=0.8.8

# Run core module tests only (25 seconds) - note: has 1 known failing test
mvn -T 1C -B test -pl sniffy-core -Djacoco.version=0.8.8
```

**Expected Test Issues**: The `SnifferSocketImplTest.testCheckConnectionAllowed` test in sniffy-core fails consistently - this is a known issue and does not prevent the build from completing.

## Validation Scenarios

**ALWAYS manually validate code changes using these scenarios:**

### Core Functionality Validation
After making changes to core modules, run:
```bash
# 1. Build core functionality
mvn -T 1C -B clean compile -pl sniffy-core,sniffy-web -Djacoco.version=0.8.8 -Dgpg.skip=true

# 2. Test Spring Boot integration with Sniffy
cd sniffy-integration-tests/sniffy-integration-tests-spring-boot
mvn test -Djacoco.version=0.8.8
```

The Spring Boot test validates:
- Embedded Tomcat server startup with Sniffy integration
- REST endpoint interception (`/restservice` and `/ouch` endpoints) 
- HTTP request filtering and processing via Sniffy
- Error handling and profiling capabilities

### Multi-Module Dependencies
The project has these key module dependencies:
- `sniffy-core` → base profiling functionality
- `sniffy-web` → depends on sniffy-core, sniffy-module-tls, sniffy-module-nio, sniffy-module-nio-compat
- `sniffy` → main module depending on sniffy-core, sniffy-web
- `sniffy-test-*` → testing integrations for JUnit, TestNG, Spring Test
- `sniffy-integration-tests-*` → comprehensive integration test suites

**Always build in dependency order or use reactor build (`mvn install`) to ensure proper module resolution.**

## Build Timing and Expectations

- **Dependency Resolution**: ~2 minutes (NEVER CANCEL - set timeout 300+ seconds)
- **Full Build (no tests)**: ~1 minute (NEVER CANCEL - set timeout 180+ seconds)
- **Full Build (with tests)**: ~2+ minutes (NEVER CANCEL - set timeout 300+ seconds)  
- **Core Module Tests**: ~25 seconds
- **Spring Boot Integration Tests**: ~13 seconds
- **CI Profile Build**: May take longer due to additional validations

**WARNING**: Do NOT use default Maven timeouts. Always specify explicit timeouts of 120+ minutes for any build operations to prevent premature cancellation.

## Project Structure

### Key Modules
- **sniffy-core**: Core profiling engine and socket interception
- **sniffy-web**: Web UI and servlet integration 
- **sniffy-module-tls**: TLS/SSL traffic decryption
- **sniffy-module-nio**: NIO socket interception
- **sniffy-test/**: Testing framework integrations (JUnit, TestNG, Spring)
- **sniffy-integration-tests/**: Comprehensive integration test suites

### Maven Profiles
- `ci`: CI/CD profile with signing and additional validations
- `jdk8`, `jdk9plus`: Java version-specific configurations
- `ide`: IntelliJ IDEA compatibility profile
- `sonatype`, `github`: Deployment profiles

### GitHub Actions Integration
The project uses GitHub Actions with:
- Java 8/11/17 compatibility testing on Ubuntu/Windows/macOS
- Multi-architecture testing (ARM, ARM64, S390X)
- Artifact uploads and code coverage reporting

## Common Issues and Solutions

### Build Failures
**JaCoCo Plugin Issues**: Always use `-Djacoco.version=0.8.8` to resolve SNAPSHOT dependency problems.

**Module Dependency Issues**: Some modules depend on artifacts not available in snapshots repository. Use full reactor build (`mvn install`) instead of individual module builds.

**Test Failures**: One known test failure in sniffy-core (`SnifferSocketImplTest.testCheckConnectionAllowed`) - use `-Dmaven.test.failure.ignore=true` to continue builds.

### Java Version Compatibility
- Primary development on Java 17
- Supports Java 8/11/17 via Maven profiles
- Some Spring Boot integration tests may have module system conflicts on Java 17 when running standalone (this is expected)

## CI Validation Requirements

Before committing changes, ALWAYS run:
```bash
# Full validation sequence (3+ minutes total) - NEVER CANCEL any step
mvn -T 1C -B clean install -Djacoco.version=0.8.8 -Dgpg.skip=true -Dmaven.test.failure.ignore=true
cd sniffy-integration-tests/sniffy-integration-tests-spring-boot
mvn test -Djacoco.version=0.8.8
```

This ensures your changes will pass the CI build pipeline which uses similar commands with the `ci` profile.

## Demo and Live Validation

**Live Demo**: https://demo.sniffy.io/owners.html?lastName=
**Documentation**: https://www.sniffy.io/docs/latest/

For local validation, the Spring Boot integration test provides a working demonstration of Sniffy's capabilities including HTTP request interception, profiling, and web UI integration.
