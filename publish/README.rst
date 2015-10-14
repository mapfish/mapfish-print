Summary
=======

This module is responsible for deploying the project to the maven repositories.  At the time of this writing it 
uploads several artifacts to the https://oss.sonatype.org/ which is the staging repository for maven central.
Once uploaded to https://oss.sonatype.org/ then it can be published to maven central for general consumption.

See http://central.sonatype.org/pages/ossrh-guide.html for details on the general process as described by 
sonatype.

Process
=======

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
  host=oss.sonatype.org
  # gpg configuration information
  signing.keyId=<id of the gpg key to use for the deploy>
  signing.password=<password for gpg key that applies to the keyId> 
  signing.secretKeyRingFile=<path to gnupg .gpg file for example: C:\\Users\\xyz\\.gnupg\\secring.gpg>
  sonatypeUsername=<sonatype username>
  sonatypePassword=<sonatype password>
  
Publish Workflow
----------------

1. Perform build to ensure that build passes all tests

.. code :: groovy

  > ./gradlew clean build

2. Upload archives to https://oss.sonatype.org/ using gradle (this will take a long time)

.. code :: groovy

  > ./gradlew uploadArchives

3. Login to https://oss.sonatype.org/
4. Click _Staging Repositories_ in Navigation along left side
5. Locate and select the mapfish-print repository
  The details will be shown in a panel below the repository list
6. Check the _Activity_ in the _Detail Section_ (the _Summary Tab_ should be visible at this point)
7. Open the _Content Tab_ in the _Detail Section_.
8. Verify all the expected artifacts have been uploaded
9. Check Repository in the repository list (if not already checked)
10. Click the _Close_ button to trigger the quality checks required by maven central
  If you see a problem them you can click the _Drop_ button to get rid of this upload
  It might take a while for the Close operation to finish so you can keep checking back or wait for the email that says the close operation has finished
11. Once Closed you have to check the _Summary_ and _Activity_ Tabs to ensure that no errors were found during the Close operation
  There will be a temporary Maven repository set up for you to test that the artifacts work correctly.  You can test this if you wish but it is not required unless you have some concern.
12. Click the _Release_ button
  It can take several hours for the artifacts to show up on Maven Central after being released.  You will just have to keep checking http://search.maven.org/ (or http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.mapfish.print%22) and see if the new version is available.
  
  
