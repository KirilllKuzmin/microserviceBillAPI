FROM openjdk:8-jdk-alpine
EXPOSE 8082
COPY build/libs/bill-service-1.0-SNAPSHOT.jar .
ENTRYPOINT ["java", "-jar", "build/libs/bill-service-1.0-SNAPSHOT.jar"]