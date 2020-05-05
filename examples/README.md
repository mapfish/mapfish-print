Summary
=======

This repository is a submodule of mapfish-print and contains e2e tests for the pdf and image generation. It
contains the code to start a Geoserver instance and run junit integration tests against the server to test as
many of the mapfish-print options as possible.

To run the integration tests:

    ./gradlew examples:geoserver

The task is a gradle test task and more details on how to run single tests or subgroups of tests can be
understood by referring to:

> <http://www.gradle.org/docs/current/userguide/java_plugin.html#sec:java_test>

Test client
===========

The test server includes a client which can be used for testing. To start the server, run:

    ./gradlew examples:farmRun

This will start a GeoServer at <http://localhost:8080/gs-web-app/> but also a MapFish Print with a simple
client that can be accessed at <http://localhost:8080/print/>

Writing Tests
=============

By default the test server is in daemon mode, which mean that the servers will be run in a background thread
and be shutdown when the build completes. In order to be able to run the tests in a IDE one can run:

    ./gradlew examples:farmRun

This will start the test servers in non-daemon mode allowing one to start the server and then run tests in
your IDE against that server for development.
