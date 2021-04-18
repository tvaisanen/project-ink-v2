FROM openjdk:8-alpine

COPY target/uberjar/project-ink-v2.jar /project-ink-v2/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/project-ink-v2/app.jar"]
