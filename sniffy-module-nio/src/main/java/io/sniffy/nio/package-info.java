/**
 * TCP NIO monitoring implementation.
 *
 * <p>The selector owns registration links in registering, active, cleaning, and removed states. A delegate key keeps
 * its link in the delegate attachment; user attachments remain on the canonical wrapper key. Close marks the selector
 * closing before closing the delegate, waits for registering links to finish or roll back, and removes ownership only
 * after the wrapper channel key has been removed.</p>
 *
 * <p>Public selection and close lock the wrapper selector and then its stable public selected-key set. Registration is
 * entered with the wrapper channel registration/key locks and may then use delegate registration locks. Cleanup runs
 * only after delegate operations release their locks, and never holds the registration-lifecycle lock while acquiring
 * a wrapper channel lock. Consumer callbacks run under the two public wrapper monitors with no delegate monitor or
 * internal provider-construction scope held.</p>
 *
 * <p>{@code JdkNioAccess} owns provider-slot access, module opening, wrapper-key removal, and compatibility calls to
 * changing {@code SelChImpl} methods. Implementing {@code SelChImpl} remains necessary because JDK selectors require
 * selectable channels to implement it; those signatures are isolated to this NIO module's compatibility adapters.</p>
 *
 * <p>Only IP TCP channels are monitored. UNIX-domain, UDP, and unknown non-IP protocol families are pass-through;
 * NIO2/AIO is unsupported.</p>
 */
package io.sniffy.nio;
