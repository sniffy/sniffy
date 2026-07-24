import { defineUseCase } from '../components/use-case/use-case';

export const trafficCaptureUseCase = defineUseCase({
  metadata: {
    accent: 'traffic',
    description:
      'Capture Socket and monitored TCP NIO traffic inside a Java application with Sniffy, with opt-in plaintext inspection for supported JSSE TLS paths.',
    relatedDocs: [
      {
        description:
          'Enable payload capture, inspect raw and decrypted packets, and review the TLS caveats.',
        href: '/docs/network/traffic-capture/',
        label: 'Capture traffic and inspect TLS',
      },
      {
        description:
          'Enable monitored IP TCP channels and review the UDP, UNIX-domain, and NIO2 boundaries.',
        href: '/docs/configuration/nio-monitoring/',
        label: 'Configure NIO monitoring',
      },
      {
        description:
          'Review the socket, packet-merging, buffering, NIO, and TLS configuration switches.',
        href: '/docs/configuration/',
        label: 'Review capture configuration',
      },
      {
        description:
          'Choose the application integration or Java agent that initializes Sniffy early enough.',
        href: '/docs/installation/',
        label: 'Install Sniffy',
      },
    ],
    route: '/use-cases/traffic-capture/',
    social: {
      description:
        'Inspect Java Socket, monitored TCP NIO, and supported JSSE TLS traffic from inside the instrumented JVM.',
      image: '/img/brand/sniffy-social.svg',
      title: 'Java traffic capture and TLS inspection with Sniffy',
    },
    title: 'Java traffic capture and TLS inspection with Sniffy',
  },
  eyebrow: 'Traffic capture and TLS inspection',
  hero: {
    body: 'Sniffy records the bytes that supported Java networking paths successfully send and receive, then keeps raw and opt-in decrypted views available to the browser experience and Spy API. The evidence comes from the instrumented JVM—not from a host-wide packet sniffer.',
    proofPoints: [
      'Capture classic Socket and opt-in monitored IP TCP NIO traffic',
      'Filter packets by thread, target, stack trace, and connection',
      'Inspect plaintext separately on supported, explicitly enabled JSSE paths',
    ],
    title: 'Follow network bytes from the Java call that moved them.',
  },
  problem: {
    body: [
      'A remote call can fail or slow down inside application code while an external packet trace lacks the Java thread, stack, and request context needed to explain why. Encrypted wire bytes add another boundary when the question concerns the plaintext a supported JSSE call consumed or produced.',
      'Host-wide capture can also be the wrong promise. A focused investigation needs evidence from the JVM under test, with an explicit account of which Socket, NIO, and TLS paths Sniffy can instrument and which paths remain outside that model.',
    ],
  },
  solution: {
    body: [
      'Create a Spy with network traffic capture enabled around the work you need to inspect. The Spy setting records payloads but does not enable classic Socket monitoring by itself: io.sniffy.monitorSocket defaults to false, so enable it explicitly for a direct Spy setup. SniffyFilter enables classic Socket monitoring by default unless monitor-socket=false is configured. The standalone javaagent enables classic Socket monitoring at startup and has no equivalent monitorSocket=false agent argument.',
      'Sniffy records successful reads and writes on monitored classic sockets and, when enabled, IP TCP SocketChannel paths. NetworkPacket values retain bytes, direction, timestamp, and optional thread or stack information for programmatic checks.',
      'TLS plaintext inspection is a separate opt-in capability. Enable TLS decryption and initialize Sniffy before application code caches JSSE objects; supported SSLSocket and SSLEngine paths publish plaintext through getDecryptedNetworkTraffic while getNetworkTraffic remains the raw transport view.',
      'The boundary is deliberate: Sniffy does not capture another process or every packet on the host. UDP DatagramChannel, UNIX-domain channels, NIO2/AIO, native transports such as explicitly enabled Netty native transport or Apache APR, and TLS stacks outside the supported JSSE interception paths are not covered.',
    ],
  },
  capabilities: [
    {
      description:
        'Record only bytes successfully read or written, with sent/received direction, timestamp, and optional thread and stack context.',
      title: 'Preserve application-level evidence',
    },
    {
      description:
        'Group or filter NetworkPacket results by target address, thread, stack trace, and Sniffy connection identity.',
      title: 'Narrow the investigation',
    },
    {
      description:
        'Cover classic Java sockets by configuration and opt into monitored IP TCP SocketChannel and ServerSocketChannel paths when NIO matters.',
      title: 'Choose the supported transport paths',
    },
    {
      description:
        'Keep encrypted transport traffic and supported JSSE plaintext in separate raw and decrypted API views instead of presenting one as the other.',
      title: 'Separate raw bytes from TLS plaintext',
    },
    {
      description:
        'Treat early initialization, supported providers, and the documented native, datagram, UNIX-domain, and asynchronous exclusions as part of the setup.',
      title: 'Make the limits visible',
    },
  ],
  codeExamples: [
    {
      code: `try (Spy<?> spy = Sniffy.spy(
    SpyConfiguration.builder()
        .captureNetworkTraffic(true)
        .build())) {
  performSocketOperation();

  Map<SocketMetaData, List<NetworkPacket>> packets =
      spy.getNetworkTraffic(
          Threads.ANY,
          AddressMatchers.anyAddressMatcher(),
          GroupingOptions.builder()
              .groupByThread(false)
              .groupByStackTrace(false)
              .groupByConnection(false)
              .build());
}`,
      label: 'Capture and group raw traffic',
      language: 'Java',
      source: {
        label: 'Core traffic capture test',
        path: 'sniffy-core/src/test/java/io/sniffy/socket/CaptureTrafficOverviewTest.java',
        region: 'CaptureTrafficOverview',
      },
    },
    {
      code: `-Dio.sniffy.monitorSocket=true
-Dio.sniffy.monitorNio=true
-Dio.sniffy.decryptTls=true`,
      label: 'Enable classic Socket, NIO, and TLS inspection',
      language: 'JVM options',
      source: {
        label: 'Current configuration reference',
        path: 'sniffy-ui/apps/site/docs/configuration/index.mdx',
      },
    },
    {
      code: `Map<SocketMetaData, List<NetworkPacket>> plaintext =
    spy.getDecryptedNetworkTraffic(
        Threads.ANY,
        AddressMatchers.anyAddressMatcher(),
        GroupingOptions.builder()
            .groupByThread(false)
            .groupByStackTrace(false)
            .groupByConnection(false)
            .build());`,
      label: 'Read the supported TLS plaintext view',
      language: 'Java',
      source: {
        label: 'TLS traffic capture test',
        path: 'sniffy-module-tls/src/test/java/io/sniffy/tls/CaptureSslTrafficTest.java',
        region: 'CaptureTrafficOverview',
      },
    },
  ],
  productResult: {
    alt: 'Current Sniffy profiler Network Connections tab showing monitored database and socket endpoints, status and delay controls, and a network byte total',
    caption:
      'The browser surface identifies endpoints and request-level network volume; packet payloads remain explicit Spy API data rather than being fabricated into this screenshot.',
    height: 540,
    highlights: [
      'Discovered socket endpoints stay visible inside the current Sniffy profiler',
      'Network-byte totals sit alongside SQL, timing, and exception evidence for the request',
      'Raw NetworkPacket data and the opt-in decrypted view remain separate programmatic results',
    ],
    provenance:
      'Byte-for-byte copy of the current profiler’s deterministic Playwright fixture after opening Network Connections; generated and reviewed in this repository, with no invented UI.',
    src: '/img/use-cases/traffic-capture-network.png',
    width: 760,
  },
  sectionHeadings: {
    capabilities: {
      eyebrow: 'Supported evidence',
      title: 'Know which bytes Sniffy can put in context.',
    },
    codeExamples: {
      eyebrow: 'Repository-backed setup',
      title: 'Capture raw traffic, then opt into supported TLS plaintext.',
    },
    cta: {
      eyebrow: 'Start inside the JVM',
      title: 'Inspect one supported network path with its limits in view.',
    },
    problem: {
      eyebrow: 'The visibility gap',
      title: 'Wire traffic alone does not explain the Java call behind it.',
    },
    productResult: {
      eyebrow: 'Real product evidence',
      title: 'Connect network activity to the monitored application.',
    },
    relatedDocumentation: {
      eyebrow: 'Configuration and limits',
      title: 'Use the current capture, NIO, setup, and configuration guides.',
    },
    solution: {
      eyebrow: 'The Sniffy boundary',
      title: 'Observe supported Java networking paths—not the whole host.',
    },
  },
  cta: {
    body: 'Start with a single Socket or monitored TCP NIO operation, enable payload capture only for the scope that needs it, and add TLS plaintext inspection only when the application uses a supported JSSE path.',
    primary: {
      href: '/docs/network/traffic-capture/',
      label: 'Read the traffic capture guide',
    },
    secondary: {
      href: '/docs/configuration/nio-monitoring/',
      label: 'Review the NIO boundary',
    },
  },
});
