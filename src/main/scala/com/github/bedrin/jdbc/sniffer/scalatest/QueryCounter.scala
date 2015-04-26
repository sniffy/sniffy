package com.github.bedrin.jdbc.sniffer.scalatest

import java.util.concurrent.Callable

import com.github.bedrin.jdbc.sniffer.{Threads, Sniffer}
import org.scalatest.{SuiteMixin, Suite}

trait QueryCounter extends SuiteMixin {

  this: Suite =>

  implicit def callable[T](f: => T): Callable[T] = new Callable[T]() { def call() = f }

  def expect[A](allowedStatements : Int)(test : => Any) =
    Sniffer.expect(allowedStatements).call(test)
  def expect[A](allowedStatements : Int, threads: Threads)(test : => Any) =
    Sniffer.expect(allowedStatements, threads).call(test)

  def expectNever[A]()(test : => Any) =
    Sniffer.expectNever().call(test)
  def expectNever[A](threads: Threads)(test : => Any) =
    Sniffer.expectNever(threads).call(test)

  def expectAtMostOnce[A]()(test : => Any) =
    Sniffer.expectAtMostOnce().call(test)
  def expectAtMostOnce[A](threads: Threads)(test : => Any) =
    Sniffer.expectAtMostOnce(threads).call(test)

  def expectAtMost[A](allowedStatements : Int)(test : => Any) =
    Sniffer.expectAtMost(allowedStatements).call(test)
  def expectAtMost[A](allowedStatements : Int, threads: Threads)(test : => Any) =
    Sniffer.expectAtMost(allowedStatements, threads).call(test)

  def expectAtLeast[A](allowedStatements : Int)(test : => Any) =
    Sniffer.expectAtLeast(allowedStatements).call(test)
  def expectAtLeast[A](allowedStatements : Int, threads: Threads)(test : => Any) =
    Sniffer.expectAtLeast(allowedStatements, threads).call(test)

  def expectBetween[A](minAllowedStatements : Int, maxAllowedStatements : Int)(test : => Any) =
    Sniffer.expectBetween(minAllowedStatements, maxAllowedStatements).call(test)
  def expectBetween[A](minAllowedStatements : Int, maxAllowedStatements : Int, threads: Threads)(test : => Any) =
    Sniffer.expectBetween(minAllowedStatements, maxAllowedStatements, threads).call(test)

}
