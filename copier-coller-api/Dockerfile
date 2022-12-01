FROM maven:3-jdk-11 AS build
COPY src /usr/src/app/src
COPY pom.xml /usr/src/app
RUN mvn -f /usr/src/app/pom.xml clean package -DskipTests

FROM openjdk:11-slim-buster
COPY --from=build /usr/src/app/target/context-sharing-api*.jar /usr/app/context-sharing-api.jar
RUN mkdir -p /app/json-schemas-repo
RUN chown -R daemon: /app/json-schemas-repo
USER daemon
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/usr/app/context-sharing-api.jar"]
