## Summary

This repository is a submodule of mapfish-print and contains e2e tests for the pdf and image generation. It
contains the code to start a Geoserver instance and run junit integration tests against the server to test as
many of the mapfish-print options as possible.

To run the integration tests:

    docker-compose up -d

The task is a gradle test task and more details on how to run single tests or subgroups of tests can be
understood by referring to:

> <http://www.gradle.org/docs/current/userguide/java_plugin.html#sec:java_test>

## Test client

The test server includes a client which can be used for testing. To start the server, run:

    docker-compose up -d

In the docker-comose context GeoServer can be accessed at <http://geoserver:8080/geoserver/> and
MapFish Print can be accessed at <http://print:8080/>

## Writing Tests

By default the test server is in daemon mode, which mean that the servers will be run in a background thread
and be shutdown when the build completes. In order to be able to run the tests in a IDE one can run:

    docker-compose up -d

This will start the test servers in non-daemon mode allowing one to start the server and then run tests in
your IDE against that server for development.

## Test images used by acceptance tests

The application config will be loaded from the `src/test/resources/examples/<tested_application>/config.yaml` file.

The spec files will be loaded from the `src/test/resources/examples/<tested_application>/requestData<postfix>.json` file.

The expected images will be loaded from the `src/test/resources/examples/<tested_application>/expected_output/requestData<postfix>.png` file.

We can optionally have a mask image that will be used to mask the tested image.
This mask image will be loaded from the `src/test/resources/examples/<tested_application>/expected_output/mask-requestData<postfix>.png` file.
This image should be grayscale with only full black and full white pixel values.
If we create the mask image with Gimp, we can't create indexed image with tow colors because in the Java code we get the values 1 instead of 0 for the black pixel.
