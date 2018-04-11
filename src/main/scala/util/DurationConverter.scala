package util

import scala.concurrent.duration.FiniteDuration

/**
  * Converts a Java Duration to a Scala FiniteDuration (used with Timers)
  */
object DurationConverter {
  implicit def asFiniteDuration(d: java.time.Duration): FiniteDuration =
    scala.concurrent.duration.Duration.fromNanos(d.toNanos)
}
