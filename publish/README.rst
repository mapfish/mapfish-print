Summary
=======

This module is responsible for deploying the project to the maven repositories.  At the time of this writing it
uploads several artifacts to the https://oss.sonatype.org/ which is the staging repository for maven central.
Once uploaded to https://oss.sonatype.org/ then it can be published to maven central for general consumption.

See http://central.sonatype.org/pages/ossrh-guide.html for details on the general process as described by
sonatype.

Automated Process
=================

1. Edit the root build.gradle to change the allprojects/version and commit that with a release/${version} tag
2. Push the commit and the tag to github


Manual Process
==============

Credential Configuration
------------------------

The first thing that must be done is to configure the gradle configuration with developers credentials
and certificates so that the gradle build can correctly log in to https://oss.sonatype.org/ and can
sign the artifacts as required.

Prerequisites
~~~~~~~~~~~~~
* GNUPG GPG key
* Sonatype account

Credential Configuration
~~~~~~~~~~~~~~~~~~~~~~~~

1. Create file: $HOME/.gradle/gradle.properties
2. File should have the following properties:

.. code ::

  # server to publish to
  enablePublishing=true
  host=oss.sonatype.org
  # gpg configuration information
  signing_keyId=<id of the gpg key to use for the deploy>
  signing_password=<password for gpg key that applies to the keyId>
  sonatypeUsername=<sonatype username>
  sonatypePassword=<sonatype password>

Publish Workflow
----------------

1. Edit the root build.gradle to change the allprojects/version and commit that with a release/${version} tag
2. Perform build to ensure that build passes all tests

.. code :: groovy

  > ./gradlew clean build

3. Upload archives to https://oss.sonatype.org/ using gradle (this will take a long time)

.. code :: groovy

  > ./gradlew publishToNexus

4. Close and release:

.. code :: groovy

  > ./gradlew closeAndReleaseRepository

5. Push the commit and the tag to github

More information about the release procedure on Sonatype can be found here: http://central.sonatype.org/pages/releasing-the-deployment.html

Docker daily build
------------------

1. Create a branch for the new version x.y
2. Activate the daily build in Travis for this branch: https://travis-ci.org/mapfish/mapfish-print/settings
