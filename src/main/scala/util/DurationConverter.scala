package util

/**
  * Created by Christopher Bradford on 3/31/18.
  */
object DurationConverter {
  implicit def asFiniteDuration(d: java.time.Duration) =
    scala.concurrent.duration.Duration.fromNanos(d.toNanos)
}
