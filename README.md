# BURST Exporter
This application monitors various aspects of the BURST cryptocurrency along with individual account. It is built around the Akka framework for fault tolerance.

## Configuration

Configuration is handled by the [Lightbend Config](https://github.com/lightbend/config) library utilizing HOCON syntax. An example configuration may be found in [`reference.conf`](https://github.com/bradfordcp/burst-exporter/blob/master/src/main/resources/reference.conf) (excerpt below).

```hocon
prometheus {
  http.port = 9000
}

burst_exporter {
  default_poll_interval = 5 seconds
  fallback_poll_interval = 30 seconds

  burst_network {
    wallet_url = "http://127.0.0.1:8125"
  }

  account_monitoring {
    wallet_url = "https://127.0.0.1:8125"

    accounts = [] // List in String format either the RS or numeric form
  }

  pool_monitoring {
    pool_url = "wss://0-100-pool.burst.cryptoguru.org/ws"

    accounts = [] // List in String format either the RS or numeric form
  }
}
```

### Detailed parameters

* `prometheus.http.port` - Port to listen for metrics requests on, defaults to `9000`. After starting the app open a browser to [http://127.0.0.1:9000/metrics](http://127.0.0.1:9000/metrics) to view all collected information.
* `burst_exporter` -
  * `default_poll_interval` - Interval in which to poll information from the wallet.
  * `fallback_poll_interval` - Interval to use when an error has been encountered.
  * `burst_network.wallet_url` - URL for a running wallet where we may pull network information. This endpoint **MUST** accept connections from the IP where the application is running.
  * `account_monitoring` - 
    * `wallet_url` - URL for a running wallet where we may pull account information. Like `burst_network.wallet_url` this endpoint **MUST** accept connections from the IP where the application is running.
    * `accounts` - List of accounts to be monitored as Strings. You may use either the RS or numeric account ID
  * `pool_monitoring` -
    * `pool_url` - Websocket URL of the BURST pool you would like to monitor. Accepts the protocol "wss://"
    * accounts

These parameters are provided in a file called `application.conf` at runtime. You do NOT need to adjust the `reference.conf` file directly. Anything provided as part of `application.conf` will override the values in `reference.conf`. The location of `application.conf` is provided as part of the command to start the application:

```bash
java -Dconfig.file=/app/application.conf -jar ./burst-exporter-assembly-0.6.jar
```

## Containers

It is simple to run this application as part of a container. Releases are packaged up as Docker containers for ease of deployment. Application configuration is handled via an exposed volume. For more information see [Docker Hub](https://hub.docker.com/r/bradfordcp/burst_exporter) and the project's [Dockerfile](https://github.com/bradfordcp/burst-exporter/blob/master/Dockerfile).

An entire setup may be managed via Docker with:

* [Prometheus](https://hub.docker.com/r/prom/prometheus/)
* [Grafana](https://hub.docker.com/r/grafana/grafana)
* [Burst Exporter](https://hub.docker.com/r/bradfordcp/burst_exporter)

_Configuration of these containers is an exercise left up to the reader._

## Building

Building the project is handled by SBT. 

```bash
sbt clean compile assembly
``` 

The resulting JAR file may be found under `target/scala-2.12/burst-exporter-assembly-version.jar`.
