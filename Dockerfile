# --- Build: biên dịch bằng Maven trong container ---
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY main ./main
RUN mvn -B -q package -DskipTests

# --- Run: chỉ cần JRE ---
FROM eclipse-temurin:17-jre
WORKDIR /app
ENV TZ=Asia/Ho_Chi_Minh
COPY --from=build /app/target/library-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
