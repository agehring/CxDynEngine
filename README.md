# Checkmarx Dynamic Engines

Service for dynamically provisioning Checkmarx engine servers based on the scan queue.

### Overview

This service is written in Java, using [Spring Boot](https://projects.spring.io/spring-boot/ "Rocks!").  It uses [Gradle](https://gradle.org/ "Is Cool!") for build and dependency management.

### Build

    ./gradlew clean build

*To skip tests:*

    ./gradlew build -x test

### Run

To run the dynamic engine service,

*From command line:*

```
java -Djasypt.encryptor.password='CxR0cks!!' -Dspring.profiles.active=aws -jar cx-dyn-engines-app-0.7.0-SNAPSHOT.jar
```

Using

*Using Gradle*

```
./gradlew bootRun or ./gradlew clean bootRun
```

