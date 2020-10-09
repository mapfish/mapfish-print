FROM gradle:6.6-jdk11 AS builder

RUN apt update
RUN apt install --yes fonts-liberation gettext curl
RUN gradle --version

ARG GIT_HEAD
ENV GIT_HEAD=${GIT_HEAD}

WORKDIR /src

COPY gradle/ ./gradle/
COPY gradle.properties build.gradle settings.gradle CI.asc ./
COPY examples/build.gradle ./examples/
COPY docs/build.gradle ./docs/
COPY publish/build.gradle ./publish/
COPY core ./core
COPY checkstyle_* ./

# '&& touch success || true' is a trick to be able to get out some artifacts
RUN gradle :core:build :core:explodedWar :core:libSourcesJar :core:libJavadocJar && touch success || true

COPY publish ./publish

RUN [ -e success ] &&  gradle :publish:build && touch success-publish || true

COPY examples ./examples
COPY docs ./docs

RUN [ -e success ] && gradle :examples:build buildDocs && touch success-examples-docs || true

FROM builder AS test-builder

RUN [ -e success ] && [ -e success-publish ] && [ -e success-examples-docs ]
