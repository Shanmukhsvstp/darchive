# ---- build ----
FROM gradle:jdk21 AS build
WORKDIR /app
COPY . .
RUN gradle bootJar --no-daemon -x test

# ---- run ----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/darchive-0.0.1.jar app.jar
# Free hosts have ~512 MB RAM: keep the JVM small.
ENV JAVA_OPTS="-Xmx300m -XX:+UseSerialGC"
EXPOSE 8080
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
