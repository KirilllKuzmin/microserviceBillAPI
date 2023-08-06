FROM openjdk:8-jdk-alpine
EXPOSE 22022
COPY build/libs/gateway-1.0-SNAPSHOT.jar .
ENTRYPOINT ["java", "-jar", "build/libs/gateway-1.0-SNAPSHOT.jar"]