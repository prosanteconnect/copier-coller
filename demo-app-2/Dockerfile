FROM maven:3-jdk-11 AS build
COPY demo-app-2/src /usr/src/app/src
COPY demo-app-2/pom.xml /usr/src/app
RUN mvn -f /usr/src/app/pom.xml clean package -DskipTests

FROM openjdk:11-slim-buster
COPY --from=build /usr/src/app/target/app2*.jar /usr/app/app2.jar
USER daemon
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/usr/app/app2.jar"]