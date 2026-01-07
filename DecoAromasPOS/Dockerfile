FROM eclipse-temurin:17-jdk
ARG JAR_FILE=target/decoaromas-sprint3-backend.jar
COPY ${JAR_FILE} decoaromas-sprint3-backend.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/decoaromas-sprint3-backend.jar"]
RUN apt-get update && apt-get install -y postgresql-client