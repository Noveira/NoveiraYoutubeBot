# ---- BUILD AŞAMASI ----
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

# ---- ÇALIŞTIRMA AŞAMASI ----
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /app/target/discord-youtube-notifier-1.0.0.jar app.jar

# SSL/TLS güvenlik protokolü açıkça tanımla
CMD ["java", \
     "-Dhttps.protocols=TLSv1.2,TLSv1.3", \
     "-Djdk.tls.client.protocols=TLSv1.2,TLSv1.3", \
     "-Dcom.sun.jndi.ldap.connect.pool=false", \
     "-jar", "app.jar"]
