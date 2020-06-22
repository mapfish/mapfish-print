FROM gradle:5.5.1-jdk8 AS builder

RUN apt update
RUN apt install --yes fonts-liberation gettext curl
RUN gradle --version

ARG GIT_HEAD
ENV GIT_HEAD=${GIT_HEAD}

WORKDIR /src

COPY gradle/ /src/gradle/
COPY gradle.properties build.gradle settings.gradle CI.asc /src/
COPY core/build.gradle /src/core/
COPY examples/build.gradle /src/examples/
COPY docs/build.gradle /src/docs/
COPY publish/build.gradle /src/publish/
RUN gradle dependencies

COPY . ./

RUN mkdir -p /usr/local/tomcat/webapps/ROOT/print-apps

# '&& touch success || true' is a trick to be able to get out some artifacts
RUN gradle build :core:explodedWar :core:libSourcesJar :core:libJavadocJar && touch success || true


FROM builder AS test-builder

RUN [ -e success ]
