FROM openjdk:17
ARG CONFIG_FILE=application.yaml
COPY target/backend*.jar /opt/app/app.jar
COPY src/main/resources/${CONFIG_FILE} /opt/app/application.yaml
USER root
EXPOSE 8085/tcp
ENV TZ="Europe/Moscow"
CMD ["java", "-jar", "/opt/app/app.jar", "--spring.config.location=file:/opt/app/application.yaml"]