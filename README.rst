.. image:: https://travis-ci.org/camptocamp/mapfish-printV3.svg?branch=development
    :target: https://travis-ci.org/camptocamp/mapfish-printV3

Please read the documentation available here:
http://mapfish.github.io/mapfish-print/#/overview


Build
-----

Execute the following command():

.. code::

  > ./gradlew build

This will build three artifacts:  print-servlet-xxx.war, print-lib.jar, print-standalone.jar


Deploy
------

The following command will build and upload all artifacts to the maven central repository.

.. code::

  > ./gradlew uploadArchives -DsshPassphrase=...


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

 > ./gradlew run -PprintArgs="-help"

.. code::

  > ./gradlew run -PprintArgs="-config examples/config.yaml -spec examples/spec.json -output ./output.pdf"


Run in Eclipse
--------------

- Create new Java Run Configuration
- Main class is org.mapfish.print.cli.Main
- Program arguments: -config samples/config.yaml -spec samples/spec.json -output $HOME/print.pdf
