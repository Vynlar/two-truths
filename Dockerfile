# Stage 1: Build the application
FROM clojure:temurin-22-tools-deps as builder

# Install curl
RUN apt-get update && apt-get install -y curl

# Install Node.js
RUN curl -fsSL https://deb.nodesource.com/setup_22.x | bash -
RUN apt-get install -y nodejs

# Copy project files into the docker image
COPY . /app
WORKDIR /app

# Install npm dependencies and build the application
RUN npm ci
RUN clojure -A:dev:shadow-cljs release app
RUN clojure -X:uberjar

# Stage 2: Run the application
FROM eclipse-temurin:22-jre

# Copy the jar file from the builder stage
COPY --from=builder /app/target/truths.jar /app/truths.jar

# Set the startup command to run your application
CMD ["java", "-cp", "/app/truths.jar", "clojure.main", "-m", "app.server-main"]