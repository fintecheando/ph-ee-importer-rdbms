FROM openjdk:17-alpine AS builder

RUN apk update && apk add wget

COPY . ph-ee-importer

WORKDIR /ph-ee-importer

RUN ./gradlew --no-daemon -q clean bootJar

WORKDIR /ph-ee-importer/target

RUN ls -lrt /ph-ee-importer/build/libs/*

RUN jar -xf /ph-ee-importer/build/libs/*.jar

WORKDIR /ph-ee-importer/target/BOOT-INF/libs

RUN wget -q https://repo1.maven.org/maven2/mysql/mysql-connector-java/8.0.29/mysql-connector-java-8.0.29.jar

# =========================================
FROM openjdk:17-alpine as paymenthub

RUN mkdir -p /app/libs

COPY --from=builder /ph-ee-importer/build/libs/*.jar /app/ph-ee-importer-rdbms.jar

COPY --from=builder /ph-ee-importer/target/BOOT-INF/lib /app/libs

WORKDIR /

EXPOSE 8200

CMD java -Dloader.path=/app/libs -jar /app/ph-ee-importer-rdbms.jar
