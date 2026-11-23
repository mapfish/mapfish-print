FROM gradle:9.3.1-jdk21 AS builder

RUN --mount=type=cache,target=/var/cache,sharing=locked \
    --mount=type=cache,target=/root/.cache \
  apt-get update && \
  apt-get install --yes --no-install-recommends fonts-liberation gettext curl

WORKDIR /src

ENV GRADLE_OPTS=-Dorg.gradle.daemon=false

# Generate the file `libnative-platform.so`
RUN --mount=type=cache,target=/home/gradle/.gradle \
  gradle --version

COPY gradle/ ./gradle/
COPY gradle.properties build.gradle settings.gradle CI.asc ./
COPY examples/build.gradle ./examples/
COPY docs/build.gradle ./docs/
COPY publish/build.gradle ./publish/
COPY core ./core
COPY publish ./publish
COPY examples ./examples
COPY docs ./docs

ARG GIT_HEAD
ENV GIT_HEAD=${GIT_HEAD}

# Exclude the tasks that will run out of the docker build (in a docker run)
RUN --mount=type=cache,target=/home/gradle/.gradle \
   gradle --parallel --exclude-task=:core:test \
   --exclude-task=:core:spotbugsMain --exclude-task=:core:checkstyleMain \
   --exclude-task=:core:spotbugsTest --exclude-task=:core:checkstyleTest --exclude-task=:core:testCLI \
   :core:build :core:explodedWar :publish:build :examples:build :docs:buildDocs :core:libSourcesJar :core:libJavadocJar

RUN mkdir -p core/build/resources/test/org/mapfish/print/ \
    && chmod -R go=u /home/gradle /tmp/mapfish-print/ . \
    && chmod o+t -R core/build/resources

# Backup cache
RUN --mount=type=cache,target=/home/gradle/.gradle \
    cp -r /home/gradle/.gradle /home/gradle/.gradle-backup
RUN mv /home/gradle/.gradle-backup /home/gradle/.gradle

# Be able to use the container with a different user
ENV GRADLE_USER_HOME=/home/gradle/
RUN chmod -R go+rw /home/gradle/

COPY checkstyle_* ./
