# Use Java 21 for compatibility with your compiled .jar
FROM eclipse-temurin:21-jdk

# Set the working directory
WORKDIR /app

# Copy your jar file into the container
COPY CodeBackend.jar /app/CodeBackend.jar

# Expose the port your app listens on (adjust if not 8080)
EXPOSE 8080

# Run your JAR file
ENTRYPOINT ["java", "-jar", "CodeBackend.jar"]
