#!/bin/bash -e

NAME="camptocamp/mapfish_print"

function publish {
    # Setup login
    openssl aes-256-cbc -K $encrypted_fb15e83f0f9f_key -iv $encrypted_fb15e83f0f9f_iv \
        -in .dockercfg.enc -out ~/.dockercfg -d

    local version=$1
    export DOCKER_VERSION=`echo "${version}" | sed -e 's/^release\///' | sed -e 's/\//_/g'`

    echo "Deploying image to docker hub for tag ${DOCKER_VERSION}"
    ./gradlew --console=plain createDocker
    docker push "${NAME}:${DOCKER_VERSION}"
}

if [ "${TRAVIS_PULL_REQUEST}" != "false" ]
then
    echo "Not deploying image for pull requests"
    exit 0
fi

if [ "${TRAVIS_BRANCH}" == "master" ]
then
  publish latest
elif [ ! -z "${TRAVIS_TAG}" ]
then
  publish "${TRAVIS_TAG}"
  if [[ "${TRAVIS_TAG}" == release/* ]]
  then
    echo "Uploading to Nexus"
    ./gradlew publishToNexus
    echo "Releasing to mvnrepository"
    ./gradlew closeAndReleaseRepository
  fi
elif [ ! -z "${TRAVIS_BRANCH}" ]
then
  publish "${TRAVIS_BRANCH}"
else
  echo "Not deploying image"
fi
