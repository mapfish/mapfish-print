#!/bin/bash -e

NAME="camptocamp/mapfish_print"

function publish {
    local version=$1
    export DOCKER_VERSION=`echo "${version}" | sed -e 's/^release\///' | sed -e 's/\//_/g'`

    echo "Deploying image to docker hub for tag ${DOCKER_VERSION}"
    ./gradlew --console=plain createDocker
    docker push "${NAME}:${DOCKER_VERSION}"
}

if [ ! -y "${CIRCLE_PULL_REQUEST}" ]
then
    echo "Not deploying image for pull requests"
    exit 0
fi

if [ "${CIRCLE_BRANCH}" == "master" ]
then
  publish latest
elif [ ! -z "${CIRCLE_TAG}" ]
then
  publish "${CIRCLE_TAG}"
  if [[ "${CIRCLE_TAG}" == release/* ]]
  then
    echo "Uploading to Nexus"
    ./gradlew publishToNexus
    echo "Releasing to mvnrepository"
    ./gradlew closeAndReleaseRepository
  fi
elif [ ! -z "${CIRCLE_BRANCH}" ]
then
  publish "${CIRCLE_BRANCH}"
else
  echo "Not deploying image"
fi
