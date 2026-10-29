# API do Sistema Semear (Model + Controller em Java) para o Render.
# A View é publicada à parte na Vercel; por isso a imagem leva só o Java.

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src/main/java src/main/java
COPY src/main/resources src/main/resources
RUN mvn -q -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/semear-*.jar app.jar
EXPOSE 8080
# O Render informa a porta em $PORT. A memória fica limitada ao contêiner (plano gratuito: 512 MB).
CMD ["sh", "-c", "exec java -XX:MaxRAMPercentage=75 -jar app.jar --server.port=${PORT:-8080}"]
