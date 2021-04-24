FROM clojure:openjdk-8-lein AS builder

RUN apt-get update -yq  &&\
    apt-get upgrade -yq &&\
    apt-get install nodejs npm build-essential -yq

RUN npm -v

COPY . .

RUN lein uberjar

RUN pwd
RUN ls
RUN ls target

FROM openjdk:8-alpine

COPY --from=builder /tmp/target/uberjar/project-ink-v2.jar /project-ink-v2/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/project-ink-v2/app.jar"]
