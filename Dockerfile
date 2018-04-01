FROM "openjdk:8-jre-alpine"

WORKDIR /app
EXPOSE 9000

ENV CONFIG_FILE /app/application.conf

COPY target/scala-2.12/burst-exporter-assembly-0.2.jar /app/

CMD [ "java", "-jar", "./burst-exporter-assembly-0.2.jar", "-Dconfig.file=$CONFIG_FILE" ]
