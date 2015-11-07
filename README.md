# Yaourt IAT

IAT powered by Clojure, Clojurescript and Om-next


Development :

```
git clone https://github.com/Artesonraju/yaourt-iat
make
```

Deployment :

JAR generation

```
lein uberjar
```

or Docker

```
cd ./yaourt-iat
docker build -t <user>/yaourt-iat .
docker run --name <name> -v <path>/<conf-file>:/conf -p <host-port>:<container-port> <user>/yaourt-iat <container-port> /conf/<conf-file>
```


See resources/conf/conf.edn for a configuration example



Based on [om-next-starter](https://github.com/jdubie/om-next-starter)