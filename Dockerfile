# ---- BUILD AŞAMASI ----
# Maven + Java 17 ile projeyi derle
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /app

# Önce pom.xml kopyala (dependency cache için)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Kaynak kodları kopyala ve derle
COPY src ./src
RUN mvn clean package -DskipTests -B

# ---- ÇALIŞTIRMA AŞAMASI ----
# Sadece JRE ile çalıştır (daha küçük image)
FROM eclipse-temurin:17-jre

WORKDIR /app

# Derlenen JAR'ı kopyala
COPY --from=builder /app/target/discord-youtube-notifier-1.0.0.jar app.jar

# Botu başlat
CMD ["java", "-jar", "app.jar"]
