FROM public.ecr.aws/docker/library/maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package

FROM public.ecr.aws/docker/library/eclipse-temurin:21-jre AS runner

WORKDIR /app

COPY --from=builder /app/target/auth-gateway-0.0.1-SNAPSHOT.jar ./app.jar

EXPOSE 8000

ENTRYPOINT ["java", "-jar", "app.jar"]
