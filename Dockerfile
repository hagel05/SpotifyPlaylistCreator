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
RUN mkdir -p /app /etc/nginx/templates

# Copy backend JAR
COPY --from=backend-builder /build/backend/build/libs/*.jar /app/app.jar

# Copy frontend dist to nginx
COPY --from=frontend-builder /build/dist /usr/share/nginx/html
COPY frontend/nginx.conf.template /etc/nginx/templates/default.conf.template

# Setup startup script to run both Nginx and Java app
RUN cat > /start.sh << 'EOF'
#!/bin/sh
set -e

echo "Substituting Nginx template variables..."
# Manually process the nginx template with environment variable substitution
envsubst < /etc/nginx/templates/default.conf.template > /etc/nginx/conf.d/default.conf

echo "Starting Nginx..."
nginx -t
nginx -g "daemon off;" &
NGINX_PID=$!
echo "Nginx started with PID $NGINX_PID"
sleep 2

# Check if nginx is still running
if ! kill -0 $NGINX_PID 2>/dev/null; then
  echo "ERROR: Nginx failed to start"
  cat /var/log/nginx/error.log
  exit 1
fi

echo "Starting Java application..."
exec java $JAVA_OPTS -jar /app/app.jar
EOF
chmod +x /start.sh

EXPOSE 30075 30055
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
ENV SERVER_PORT=30075
ENV FRONTEND_PORT=30055
ENV BACKEND_HOST=localhost
ENV BACKEND_PORT=30075

CMD ["/start.sh"]
