import { defineUseCase } from '../components/use-case/use-case';

export function createNetworkFaultTestingUseCase(productVersion: string) {
  const version = productVersion.trim();
  if (version.length === 0 || version === 'undefined') {
    throw new Error('The network fault testing page requires the repository product version.');
  }

  return defineUseCase({
    metadata: {
      accent: 'network',
      description:
        'Test how Java applications handle blocked TCP connections and fixed Sniffy latency, with discovered endpoints and explicit test APIs.',
      relatedDocs: [
        {
          description:
            'Review how fixed delay is estimated for connects, TCP reads and writes, urgent data, and close.',
          href: '/docs/network/fault-emulation/',
          label: 'Understand fault emulation',
        },
        {
          description:
            'Install the integration that initializes Sniffy for your application or test framework.',
          href: '/docs/installation/',
          label: 'Install Sniffy',
        },
        {
          description:
            'Enable socket or TCP NIO monitoring and the separate fault-injection switch.',
          href: '/docs/configuration/',
          label: 'Configure fault injection',
        },
        {
          description:
            'Use the JUnit rule or Jupiter extension and scope socket disabling around a test invocation.',
          href: '/docs/testing/junit/',
          label: 'Integrate with JUnit',
        },
      ],
      route: '/use-cases/network-fault-testing/',
      social: {
        description:
          'Exercise Java failure handling with blocked TCP connections or deterministic fixed-delay heuristics—not arbitrary network chaos.',
        image: '/img/brand/sniffy-social.svg',
        title: 'Java network fault and resilience testing with Sniffy',
      },
      title: 'Java network fault and resilience testing with Sniffy',
    },
    eyebrow: 'Network fault and resilience testing',
    hero: {
      body: 'Sniffy discovers endpoints used by monitored Java networking paths, lets a developer block or delay those destinations in the profiler, and exposes explicit test integrations for repeatable connection failures. It does not inject packet loss, shape bandwidth, or generate random chaos.',
      proofPoints: [
        'Discover observed remote host and port pairs before choosing a fault',
        'Block a destination or apply a deterministic fixed-delay value',
        'Turn network isolation into an explicit automated-test boundary',
      ],
      title: 'Exercise failure paths without pretending to be the whole network.',
    },
    problem: {
      body: [
        'A successful integration test says little about timeout handling, fallback behavior, or accidental calls to remote services. Those paths remain untested when every dependency is reachable and fast.',
        'A useful test must also state what it changes. Sniffy works inside the instrumented JVM on supported classic Socket and monitored IP TCP NIO paths; it is not a host proxy, packet-loss simulator, bandwidth shaper, or probabilistic chaos engine.',
      ],
    },
    solution: {
      body: [
        'First let monitored application traffic reveal destination host and port pairs. Discovery is observation, not disruption: a newly discovered address starts allowed with status 0. In the injected profiler, a developer can then allow, block, or assign a fixed delay to a discovered destination. Those browser controls update Sniffy’s connection registry; they are exploratory controls, not test assertions.',
        'For repeatable tests, use a framework integration such as @DisableSockets. It installs a wildcard blocked status for the invocation, so attempts to use monitored outgoing connections fail with ConnectException. The registry is JVM-global in the JUnit integrations, so do not run unrelated network tests in parallel with that disabling scope.',
        'A positive registry status is a fixed delay in milliseconds, but not a constant end-to-end response time. Sniffy applies it to connection establishment and estimates additional delay from TCP-window-sized read and write cycles; direction changes on the same thread can force the next cycle, and urgent data and close have documented handling. A negative status blocks; values below -1 may delay before refusing, while -1 refuses without that added sleep.',
        'Fault injection must be enabled and the relevant transport must be monitored. The model covers supported classic Socket and monitored IP TCP NIO paths; it does not cover UDP DatagramChannel, UNIX-domain channels, NIO2/AIO, native transports, other processes, or network conditions outside the instrumented JVM.',
      ],
    },
    capabilities: [
      {
        description:
          'Observe host and port pairs used by monitored code without changing their initial allowed status.',
        title: 'Discover before disrupting',
      },
      {
        description:
          'Use the profiler to allow, refuse, or assign one fixed millisecond delay to a discovered destination while investigating behavior.',
        title: 'Explore faults in the browser',
      },
      {
        description:
          'Apply @DisableSockets through the JUnit rule or Jupiter extension so a test invocation fails on monitored outgoing connectivity.',
        title: 'Make isolation repeatable',
      },
      {
        description:
          'Model deterministic latency around connects and estimated TCP-window cycles rather than claiming packet-level timing or throughput control.',
        title: 'Use delay with its real scope',
      },
      {
        description:
          'Keep UDP, UNIX-domain, asynchronous NIO2, native transports, and host-wide network behavior outside the promise.',
        title: 'Preserve the transport boundary',
      },
    ],
    codeExamples: [
      {
        code: `<dependency>\n  <groupId>io.sniffy</groupId>\n  <artifactId>sniffy-junit-jupiter</artifactId>\n  <version>${version}</version>\n  <scope>test</scope>\n</dependency>`,
        label: 'Add the JUnit Jupiter integration',
        language: 'Maven',
        source: {
          label: 'Current JUnit documentation',
          path: 'sniffy-ui/apps/site/docs/testing/junit.mdx',
        },
      },
      {
        code: `testImplementation "io.sniffy:sniffy-junit-jupiter:${version}"`,
        label: 'Add the same integration with Gradle',
        language: 'Gradle',
        source: {
          label: 'Current JUnit documentation',
          path: 'sniffy-ui/apps/site/docs/testing/junit.mdx',
        },
      },
      {
        code: `import io.sniffy.socket.DisableSockets;\nimport io.sniffy.test.junit.jupiter.SniffyExtension;\nimport org.junit.jupiter.api.Test;\nimport org.junit.jupiter.api.extension.ExtendWith;\n\n@ExtendWith(SniffyExtension.class)\nclass NetworkTest {\n\n    @Test\n    @DisableSockets\n    void doesNotConnectToTheNetwork() {\n        // opening a new socket fails while this invocation is running\n    }\n}`,
        label: 'Block monitored connections for one test',
        language: 'Java',
        source: {
          label: 'JUnit Jupiter network example',
          path: 'sniffy-ui/apps/site/docs/testing/junit.mdx',
        },
      },
    ],
    productResult: {
      alt: 'Current Sniffy profiler Network Connections tab listing discovered database and socket destinations with allow, block, and delay controls',
      caption:
        'The profiler exposes real discovered endpoints and interactive status controls. Automated tests use explicit integrations such as @DisableSockets instead of driving this browser interface.',
      height: 540,
      highlights: [
        'Observed database and socket destinations remain identified by host and port',
        'Allow, block, and fixed-delay states can be selected for an endpoint',
        'The controls affect supported networking inside the instrumented application—not the host network',
      ],
      provenance:
        'The existing site asset is a byte-for-byte copy of the current profiler’s deterministic Playwright fixture after opening Network Connections; its repository source and regeneration command are documented beside the asset.',
      src: '/img/use-cases/traffic-capture-network.png',
      width: 760,
    },
    sectionHeadings: {
      capabilities: {
        eyebrow: 'Deliberate fault controls',
        title: 'Separate discovery, exploration, and test automation.',
      },
      codeExamples: {
        eyebrow: 'Repository-backed test setup',
        title: 'Make an unavailable network an explicit test condition.',
      },
      cta: {
        eyebrow: 'Start with one dependency',
        title: 'Test the fallback you expect to trust.',
      },
      problem: {
        eyebrow: 'The resilience gap',
        title: 'Reachable dependencies leave failure behavior unproven.',
      },
      productResult: {
        eyebrow: 'Real product evidence',
        title: 'Choose faults for endpoints the application actually discovered.',
      },
      relatedDocumentation: {
        eyebrow: 'Scope and setup',
        title: 'Verify the delay model, configuration, and test lifecycle.',
      },
      solution: {
        eyebrow: 'The Sniffy model',
        title: 'Control supported JVM connections with precise boundaries.',
      },
    },
    cta: {
      body: 'Pick one outgoing dependency, prove the application’s fallback with a blocked connection, then use fixed latency only when the documented TCP-window heuristic matches the behavior you need to exercise.',
      primary: { href: '/docs/network/fault-emulation/', label: 'Review the delay model' },
      secondary: { href: '/docs/testing/junit/', label: 'Set up a network test' },
    },
  });
}
