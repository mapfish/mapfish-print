FROM gradle:6.9.4-jdk11 AS builder

RUN --mount=type=cache,target=/var/cache,sharing=locked \
    --mount=type=cache,target=/root/.cache \
  apt-get update && \
  apt-get install --yes --no-install-recommends fonts-liberation gettext curl && \
  gradle --version

WORKDIR /src

COPY gradle/ ./gradle/
COPY gradle.properties build.gradle settings.gradle CI.asc ./
COPY examples/build.gradle ./examples/
COPY docs/build.gradle ./docs/
COPY publish/build.gradle ./publish/
COPY core ./core

RUN --mount=type=cache,target=/home/gradle/.gradle \
   gradle :core:processResources :core:classes
COPY checkstyle_* ./
# '&& touch success || true' is a trick to be able to get out some artifacts
RUN --mount=type=cache,target=/home/gradle/.gradle \
   (gradle :core:checkstyleMain :core:spotbugsMain :core:violations --stacktrace) \
   && ( (gradle :core:build :core:explodedWar :core:libSourcesJar :core:libJavadocJar > /tmp/logs 2>&1 && touch success) || true)

ARG GIT_HEAD
ENV GIT_HEAD=${GIT_HEAD}

COPY publish ./publish

RUN --mount=type=cache,target=/home/gradle/.gradle \
   ([ -e success ] && ( (gradle :publish:build >> /tmp/logs 2>&1) && touch success-publish)) || true

COPY examples ./examples
COPY docs ./docs

RUN --mount=type=cache,target=/home/gradle/.gradle \
   ([ -e success ] && ( (gradle :examples:build buildDocs >> /tmp/logs 2>&1) && touch success-examples-docs)) || true

RUN chmod -R go=u /home/gradle .

FROM builder AS test-builder

RUN cat /tmp/logs && ls success success-publish success-examples-docs

VOLUME [ "/src/core" ]
