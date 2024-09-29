FROM openjdk:17
COPY target/load-otus*.jar /opt/app/app.jar
USER root
EXPOSE 8085/tcp
ENV TZ="Europe/Moscow"
CMD ["java", "-jar", "/opt/app/app.jar"]