#FROM maven:3.8.5-eclipse-temurin-11-alpine@sha256:b4083a13f534f6256179802b3deaeadccf4b8eb9bcb4c8a35b78919624b18448 AS builder
FROM maven:3.8.5-eclipse-temurin-11-focal@sha256:a5a8b7b6ae4b7045c42ab9c21b72cdffe43db8eebf8bbf5a741f2660453c86ba AS base

RUN apt-get update && \
  apt-get install --yes --no-install-recommends fonts-liberation gettext curl && \
  apt-get clean && \
  rm -rf /var/lib/apt/lists/* && \
  mvn --version

WORKDIR /src

COPY pom.xml ./
COPY core ./core
COPY servlet ./servlet

FROM base AS builder
RUN mvn -B dependency:resolve -projects core

FROM builder AS cachedbuiler

# COPY checkstyle_* ./
# COPY gradle.properties build.gradle settings.gradle CI.asc ./
# COPY examples/build.gradle ./examples/
# COPY docs/build.gradle ./docs/
# COPY publish/build.gradle ./publish/

# '&& touch success || true' is a trick to be able to get out some artifacts
#RUN (gradle :core:build :core:explodedWar :core:libSourcesJar :core:libJavadocJar && touch success) || true
RUN mvn -B clean install

# ARG GIT_HEAD
# ENV GIT_HEAD=${GIT_HEAD}

# COPY publish ./publish

# RUN ([ -e success ] && (gradle :publish:build && touch success-publish)) || true

# COPY examples ./examples
# COPY docs ./docs

# RUN ([ -e success ] && (gradle :examples:build buildDocs && touch success-examples-docs)) || true

# FROM builder AS test-builder

# RUN [ -e success ] && [ -e success-publish ] && [ -e success-examples-docs ]

# VOLUME [ "/src/core" ]
