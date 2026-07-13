
# Capture and decryption pipeline for maintainers

Traffic capture crosses the core socket, optional NIO, and optional TLS
modules. The following pipeline is part of the maintenance contract; changes
should be reviewed against raw-byte identity, connection grouping, and ordering
rather than only against the public `Spy` enablement API.

## Classic socket raw TCP capture

Classic monitored sockets are represented by `SniffySocket` or
`CompatSnifferSocketImpl`. Their `SnifferInputStream`/`SnifferOutputStream`
wrappers copy only the bytes successfully read or written and call the
connection's `logTraffic` method. That method publishes raw TCP bytes through
`Sniffy.logTraffic` when the effective `SpyConfiguration` enables network
traffic capture. Socket timing/byte counters can remain enabled independently
of raw payload capture.

One connection ID is allocated for the monitored transport and reused by its
input and output paths. Raw packets use the physical remote
`InetSocketAddress`. `SocketMetaData` combines that address, the connection ID,
protocol, and the requested grouping dimensions; `NetworkPacket` adds
direction, timestamp, bytes, and optional thread/stack data.

## NIO channel and socket-stream raw TCP capture

`SniffySocketChannel` owns the monitored NIO transport. `socket()` returns one
stable `SniffySocketChannelSocket`, and that socket's streams route physical
I/O back through the channel via `SharedConnectionIO`. Channel calls and
socket-stream calls therefore share the same connection ID, physical address,
endpoint policy, byte counters, and
`connectionReadLock`/`connectionWriteLock`.

For a single `ByteBuffer`, Sniffy snapshots the starting position, performs the
delegate operation, and copies exactly the successful position advance through
a duplicate buffer so the application's position and limit remain those set by
the delegate. Gathering writes and scattering reads snapshot every
participating buffer. After the delegate returns, Sniffy walks only the
requested offset/length range and copies each buffer's actual position delta in
buffer order. It never captures untouched remaining capacity or excluded
buffers.

Each direction lock covers the physical operation, immutable byte copy,
accounting, and raw publication. The write lock also covers proxy inspection.
This makes captured order equal physical order across mixed channel/stream
calls; the separate read and write locks retain full-duplex I/O.

## HTTP CONNECT buffering and publication

NIO CONNECT detection is bounded, incremental, and post-write. Before the first
outbound protocol decision, `SocketChannelOutboundTraffic` buffers at most 8192
successfully written bytes even when capture is disabled, because endpoint
fault policy still needs the detected target. It waits while the bytes remain a
possible `CONNECT ` prefix, then asks `SniffyPacketAnalyzer` to parse a
complete header or the bounded candidate.

The write that completes detection is physically transmitted and accounted
under the policy snapshot taken at the start of that operation. Only afterward
does parsing atomically publish the proxied target address and status for the
next operation. When payload capture is enabled, a recognized CONNECT header is
published as a raw handshake segment; bytes after the header are published
separately as tunneled TCP bytes. A pending incomplete prefix is finalized once
on output shutdown or channel close. Both raw segments retain the physical
proxy address and transport connection ID.

## SSLEngine ClientHello correlation

On its first `wrap` that produces encrypted bytes, `SniffySSLEngine` stores the
produced ClientHello bytes in `Sniffy.CLIENT_HELLO_CACHE` with the engine as
the value. The first captured non-CONNECT outbound chunk on a classic or NIO
transport looks up those exact encrypted bytes. A match attaches that
transport's `SniffyNetworkConnection` to the engine. NIO deliberately excludes
the CONNECT handshake from the one-shot lookup, so the first tunneled
ClientHello can correlate TLS-over-CONNECT; direct TLS uses the first physical
outbound chunk.

The lookup is one-shot even on a miss or callback failure. This prevents later
coincidental byte sequences from reassigning an established transport. The
cache is bounded and is correlation metadata, not a second owner of the socket
lifecycle.

## Plaintext extraction and publication

`SniffySSLEngine` observes plaintext around successful, non-handshaking
`wrap`/`unwrap` calls. Single-buffer calls copy exactly `bytesConsumed()` or
`bytesProduced()` through duplicate buffers. Gathering `wrap` and scattering
`unwrap` snapshot all participating positions, then copy only actual position
advances up to the aggregate count reported by `SSLEngineResult`, once and in
buffer order. Partial consumption/production cannot copy untouched capacity,
duplicate a buffer, change application positions, or pass a publication length
larger than the temporary array.

The engine calls `logDecryptedTraffic` on its correlated
`SniffyNetworkConnection`. That reuses the raw transport's connection ID.
Direct TLS uses the physical peer address; NIO TLS-over-CONNECT uses the
proxied target address for decrypted packets while raw CONNECT/tunnel bytes
remain grouped under the physical proxy address. Thus callers can relate
encrypted and decrypted views by connection ID without treating the CONNECT
header as TLS data.

`SniffySSLSocket` is the JSSE socket-wrapper path rather than the
`CLIENT_HELLO_CACHE` engine path. Its wrapped streams observe plaintext
before/after the delegate `SSLSocket` and publish it with
`Sniffy.logDecryptedTraffic` using the SSL socket wrapper's connection ID and
chosen peer address. Do not assume that this wrapper ID is the raw
classic-socket ID; the explicit cache correlation guarantee applies to
`SniffySSLEngine` and a `SniffyNetworkConnection`.

Within one NIO direction, raw capture, CONNECT parsing, and correlation follow
physical wire order. Within one `SSLEngine` call, plaintext follows
participating buffer order and is published once. `packetMergeThreshold` may
later merge adjacent compatible `NetworkPacket` records for presentation; it
does not change which bytes the capture stages observed.
