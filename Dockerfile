# Official Maven image with JDK 17 to build application
FROM maven:3.8.5-openjdk-17-slim AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

# Create the JAR file
RUN mvn clean package -DskipTests

# Create a lightweight runtime image based on JDK 17
FROM amazoncorretto:17-alpine3.23-jdk

WORKDIR /app

# Copy the JAR file from the build stage to the runtime container
COPY --from=build /app/target/atk-nomina-batch-*.jar ./app.jar

EXPOSE 8080

# Command to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]