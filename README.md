Please read the documentation available here: <https://mapfish.github.io/mapfish-print/>

For support or questions post on the community mailing list:
<https://groups.google.com/forum/#!forum/mapfish-print-users>

The community mailing list is a place where users can share experiences, it is not a professional support
mailing list. For professional support, users should sign a support contract with a professional services
company, and use their appropriate channel.

Prerequisites
=============

Building mapfish-print requires java 8 and gradle 5.5.1.

Build
=====

Execute the following command():

``` {.sourceCode .}
> ./gradlew build
```

This will build three artifacts: print-servlet-xxx.war, print-lib.jar, print-standalone.jar

The build also builds the documentation in the docs/build/site folder. To deploy the documentation it should
simply be copied to the gh-pages branch and then committed GitHub will automatically build the updated site
at: <https://mapfish.github.io/mapfish-print/>

If you only want to build the docs simply run:

``` {.sourceCode .}
> ./gradlew docs:build
```

or run build in the docs directory.

<div class="admonition note">

The test (part of the build) requires the 'Liberation Sans' font, which can be downloaded
[here](https://www.fontsquirrel.com/fonts/Liberation-Sans).

</div>

Deploy
======

The following command will build and upload all artifacts to the maven central repository.

``` {.sourceCode .}
> ./gradlew uploadArchives -DsshPassphrase=...
```

You can as well generate a docker image using this command:

``` {.sourceCode .}
> ./gradlew createDocker
```

To use in Eclipse
=================

Create Eclipse project metadata:

``` {.sourceCode .}
> ./gradlew eclipse
```

Import project into Eclipse

Run from commandline
====================

The following command will run the mapfish printer. The arguments must be supplied to the -PprintArgs="..."
parameter.

To list all the commandline options then execute (the current direstory is ./core):

``` {.sourceCode .}
> ./gradlew print -PprintArgs="-help"
```

``` {.sourceCode .}
> ./gradlew print -PprintArgs="-config ../examples/src/test/resources/examples/simple/config.yaml -spec ../examples/src/test/resources/examples/simple/requestData.json -output ./output.pdf"
```

If you want to run in debug mode you can do the following:

``` {.sourceCode .}
> ./gradlew print --debug-jvm -PprintArgs="-config ../examples/src/test/resources/examples/simple/config.yaml -spec ../examples/src/test/resources/examples/simple/requestData.json -output ./output.pdf"
```

Run using gretty/jettyRun
=========================

The following command will run mapfish print using gretty/jetty. The default port is 8080, but can be changed
using -PhttpPort="..." parameter.

``` {.sourceCode .}
> ./gradlew jettyRun -PhttpPort=8090
```

Run in Eclipse
==============

-   Create new Java Run Configuration
-   Main class is org.mapfish.print.cli.Main
-   Program arguments: -config samples/config.yaml -spec samples/spec.json -output \$HOME/print.pdf

Contributor License Agreement
=============================

Before accepting a contribution, we ask that you provide us a Contributor License Agreement. If you are making
your contribution as part of work for your employer, please follow the guidelines on submitting a [Corporate
Contributor License Agreement](https://github.com/mapfish/mapfish-print/wiki/C2C_Corporate-CLA_v1-0.pdf). If
you are making your contribution as an individual, you can submit a digital [Individual Contributor License
Agreement](http://goo.gl/forms/QO9UELxM9m).

Credits
=======

![image](https://www.yourkit.com/images/yklogo.png)

> target
>
> :   <https://www.yourkit.com/java/profiler/index.jsp>
>
Thanks to [YourKit](https://www.yourkit.com/java/profiler/index.jsp) for letting us use their Java profiler!
