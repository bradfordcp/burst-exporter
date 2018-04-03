FROM "openjdk:8-jre-alpine"

WORKDIR /app
VOLUME /app/application.conf
EXPOSE 9000

COPY target/scala-2.12/burst-exporter-assembly-0.4.jar /app/

CMD [ "java", "-Dconfig.file=/app/application.conf", "-jar", "./burst-exporter-assembly-0.4.jar"]
