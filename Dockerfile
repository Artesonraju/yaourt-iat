FROM clojure:latest

RUN mkdir -p /usr/src/app

WORKDIR /usr/src/app

COPY project.clj /usr/src/app/

COPY ./resources /usr/src/app/resources

COPY ./src /usr/app/src

RUN lein deps

RUN mv "$(lein uberjar | sed -n 's/^Created \(.*standalone\.jar\)/\1/p')" app-standalone.jar

EXPOSE 8080

CMD ["java", "-jar", "app-standalone.jar"]