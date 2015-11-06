FROM clojure:latest

RUN mkdir -p /usr/app

WORKDIR /usr/app

COPY project.clj /usr/app/

COPY ./resources /usr/app/resources

COPY ./src /usr/app/src

RUN lein deps

RUN ls

RUN mv "$(lein uberjar | sed -n 's/^Created \(.*standalone\.jar\)/\1/p')" yaourt-iat.jar

EXPOSE 10555

VOLUME ["/conf"]

ENTRYPOINT ["java", "-jar", "yaourt-iat.jar"]