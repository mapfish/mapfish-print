#!/bin/bash -e

NAME="camptocamp/mapfish_print"

function publish {
    local version=$1
    docker_version=`echo "${version}" | sed -e 's/^release\///' | sed -e 's/\//_/g'`

    echo "Deploying image to docker hub for tag ${docker_version}"
    ./gradlew --console=plain createDocker
    docker tag "camptocamp/mapfish_print:latest" "camptocamp/mapfish_print:${docker_version}"
    docker push "${NAME}:${docker_version}"
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
