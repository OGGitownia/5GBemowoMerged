# Stage 1: Build frontend (React + Vite)
FROM node:20-slim as frontend-builder
WORKDIR /app/5GBemowoFrontend
COPY 5GBemowoFrontend/package*.json ./
RUN npm install
COPY 5GBemowoFrontend/ ./
RUN npm run build

# Stage 2: Build backend (Kotlin + Gradle)
FROM eclipse-temurin:21-jdk as backend-builder
WORKDIR /app

# Copy backend source code
COPY 5GBemowoBackend/ ./5GBemowoBackend/

# Copy compiled frontend to backend's static resources
COPY --from=frontend-builder /app/5GBemowoFrontend/dist ./5GBemowoBackend/src/main/resources/static

# Copy resources (Python scripts)
COPY resourcesShared/ ./resourcesShared/

# Build the backend
WORKDIR /app/5GBemowoBackend
COPY 5GBemowoBackend/gradlew ./gradlew
COPY 5GBemowoBackend/gradle ./gradle
RUN ./gradlew build -x test --no-daemon

# Stage 3: Final image with backend JAR + Python
FROM eclipse-temurin:21-jdk as final
WORKDIR /app

# Install Python + pip
RUN apt-get update && apt-get install -y python3 python3-pip python3-venv

# Create Python virtual environment and install dependencies
COPY requirements.txt .
RUN python3 -m venv /venv \
 && . /venv/bin/activate \
 && pip install --upgrade pip \
 && pip install --no-cache-dir -r requirements.txt

ENV PATH="/venv/bin:$PATH"

# Copy compiled JAR and resources
COPY --from=backend-builder /app/5GBemowoBackend/build/libs/*.jar app.jar
COPY --from=backend-builder /app/resourcesShared ./resourcesShared

# Environment variables
ENV SPRING_PROFILES_ACTIVE=docker
ENV DB_PASSWORD=SecurePassword123

# Expose backend (we do not need frontend port Exposed)
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
