Summary
-------
This repository is a submodule of mapfish-print and contains e2e tests for the pdf and image generation.  It contains the code to
start a Geoserver instance and run junit integration tests against the server to test as many of the mapfish-print options as possible.

Due to the difficulty of verification there are two modes of execute.

An interactive mode and a automated mode.  The interactive mode contains steps where a developer has to validate the responses.

To use module:
.. code::

    git clone --recurse-submodules https://github.com/mapfish/mapfish-print.git
    cd mapfish-print
    ./gradlew interactiveTest # for interactive and automated tests
    ./gradlew test # for automated tests only
    
    
Both tasks are gradle test tasks and more details on how to run single tests or subgroups of tests can be understood by referring to:

    http://www.gradle.org/docs/current/userguide/java_plugin.html#sec:java_test


Test Server and SamplePages
---------------------------
The geoserver in this project is configured with several webpages that show maps requesting tiles in the various layer types
(TMS, WMS, WMTS, Vector, OSM, etc...)  These pages can be used to see how openlayer interacts with these different layer types.  The
pages also have print buttons for submitting a print request for the given map.

To experiment with these pages:

1. Open a terminal
2. `./gradlew jettyRunForeground`
3. Open a web browser
4. Enter `http://localhost:9876/e2egeoserver` in the location input

The main page contains a list of all the pages and provides links to the respective pages.  Each page has a short description
explaining what should be visible and printed.

Writing Tests
-------------

There are two types of tests.  All classes that end in _InteractiveTest_ will be ran only during the testInteractive task.  All other junit
tests will be ran during the normal test task.

By default the test server is in daemon mode, which mean that the servers will be run in a background thread and be shutdown when
the build completes.  In order to be able to run the tests in a IDE there is a one can run:
.. code::

     ./gradlew jettyRunForeground

This will start the test servers in non-daemon mode allowing one to start the server and then run tests you IDE against that server for
development.
