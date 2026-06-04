# Stage 1: Build backend (Java)
FROM eclipse-temurin:21-jdk-alpine AS backend-builder
WORKDIR /build
COPY . .
RUN chmod +x gradlew && ./gradlew build -x test

# Stage 2: Build frontend (Node)
FROM node:20-alpine AS frontend-builder
WORKDIR /build
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ .
ENV VITE_API_URL=/api
RUN npm run build

# Stage 3: Runtime (Java + Nginx)
FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache nginx gettext

# Create directories
RUN mkdir -p /app /etc/nginx/conf.d

# Copy backend JAR
COPY --from=backend-builder /build/backend/build/libs/*.jar /app/app.jar

# Copy frontend dist to nginx
COPY --from=frontend-builder /build/dist /usr/share/nginx/html

# Copy nginx template (will be processed at startup with actual FRONTEND_PORT value)
COPY frontend/nginx.conf.template /etc/nginx/conf.d/default.conf.template

# Create startup script
RUN cat > /start.sh << 'START_SCRIPT'
#!/bin/sh
set -e

echo "Generating Nginx config with FRONTEND_PORT=${FRONTEND_PORT}..."
sed "s/\${FRONTEND_PORT}/${FRONTEND_PORT}/g" /etc/nginx/conf.d/default.conf.template > /etc/nginx/conf.d/default.conf

echo "Validating Nginx config..."
nginx -t

echo "Starting Nginx..."
nginx -g "daemon off;" &
NGINX_PID=$!
echo "Nginx started with PID $NGINX_PID"

sleep 2
if ! kill -0 $NGINX_PID 2>/dev/null; then
  echo "ERROR: Nginx failed to start"
  exit 1
fi

echo "Starting Java application..."
exec java $JAVA_OPTS -jar /app/app.jar
START_SCRIPT
chmod +x /start.sh

EXPOSE 30055 30075
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
ENV SERVER_PORT=30075
ENV FRONTEND_PORT=30055

CMD ["/start.sh"]
