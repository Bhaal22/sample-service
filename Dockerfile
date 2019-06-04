FROM openjdk:8-jre-alpine
WORKDIR /usr/share
COPY target/payment-service-0.0.1-SNAPSHOT.jar /usr/share/app.jar
ARG CONFIG
COPY deployment/config/${CONFIG} application.yaml
ARG LOG_DIR='/var/log/brickparking/payment-service'
RUN echo 'LOG_DIR = '
RUN echo $LOG_DIR

ENTRYPOINT ["/usr/bin/java", "-jar",  "-Dlogging.path=$LOG_DIR", "app.jar", "&"]
