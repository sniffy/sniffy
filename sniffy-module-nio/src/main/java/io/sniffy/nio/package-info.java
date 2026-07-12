/**
 * TCP NIO monitoring implementation.
 *
 * <p>Each delegate key owns one {@code SelectionKeyLink} in its delegate attachment. The link safely publishes one
 * fully initialized wrapper key; user attachments remain on that wrapper. A selector keeps only active links needed
 * for deterministic cancellation cleanup and removes them after delegate deregistration.</p>
 *
 * <p>Lock order is wrapper channel registration lock, wrapper channel key lock, then delegate registration locks.
 * Selection cleanup starts only after delegate selection has released delegate locks and may then take the wrapper
 * key lock. Code must never hold a delegate channel or selector lock while acquiring a wrapper lock. Wrapper
 * cancellation is propagated before selection; delegate cancellation and channel closure are reconciled after every
 * successful select variant. Selector close closes the delegate once and then performs the same link cleanup.</p>
 *
 * <p>{@code JdkNioAccess} owns provider-slot access, module opening, wrapper-key removal, and compatibility calls to
 * changing {@code SelChImpl} methods. Implementing {@code SelChImpl} remains necessary because JDK selectors require
 * selectable channels to implement it; those signatures are isolated to this NIO module's compatibility adapters.</p>
 */
package io.sniffy.nio;
