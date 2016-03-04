.. image:: https://travis-ci.org/mapfish/mapfish-print.svg?branch=master
    :target: https://travis-ci.org/mapfish/mapfish-print

Please read the documentation available here:
http://mapfish.github.io/mapfish-print/

For support or questions post on the mailing list:
https://groups.google.com/forum/#!forum/mapfish-print-users

Build
-----

Execute the following command():

.. code::

  > ./gradlew build

This will build three artifacts:  print-servlet-xxx.war, print-lib.jar, print-standalone.jar

The `build` also builds the documentation in the `docs/build/site` folder.  To deploy the documentation it should simply be copied to the gh-pages
branch and then committed github will automatically build the updated site at: http://mapfish.github.io/mapfish-print/#/overview

If you only want to build the docs simply run

.. code::

  > ./gradlew docs:build

or run build in the docs directory

Deploy
------

The following command will build and upload all artifacts to the maven central repository.

.. code::

  > ./gradlew uploadArchives -DsshPassphrase=...

You can as well generate a docker image using this command:

.. code::

  > ./gradlew createDocker

If you want to force the version to something custom, you can define the DOCKER_VERSION environment
variable.

To use in Eclipse
-----------------

Create Eclipse project metadata:

.. code::

  > ./gradlew eclipse

Import project into Eclipse


Run from commandline
--------------------

The following command will run the mapfish printer.  The arguments must be supplied to the -PprintArgs="..." parameter.

To list all the commandline options then execute:

.. code::

 > ./gradlew print -PprintArgs="-help"

.. code::

  > ./gradlew print -PprintArgs="-config examples/config.yaml -spec examples/spec.json -output ./output.pdf"

If you want to run in debug mode you can do the following:

.. code::
  > ./gradlew print --debug-jvm -PprintArgs="-config examples/config.yaml -spec examples/spec.json -output ./output.pdf"


Run in Eclipse
--------------

- Create new Java Run Configuration
- Main class is org.mapfish.print.cli.Main
- Program arguments: -config samples/config.yaml -spec samples/spec.json -output $HOME/print.pdf
