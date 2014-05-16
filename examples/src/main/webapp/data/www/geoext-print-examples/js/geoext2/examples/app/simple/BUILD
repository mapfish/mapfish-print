=== Install the SDK ===

Download and Install Sencha SDK tools.
http://www.sencha.com/products/sdk-tools/download/

On Linux:

  chmod +x SenchaSDKTools-2.0.0-beta3-linux.run
  sudo SenchaSDKTools-2.0.0-beta3-linux.run

=== Download ExtJs ===

Download Ext JS 4.2.1 from http://www.sencha.com/products/extjs/download,
and unzip it in the current dir (examples/app/simple/ext-4.2.1.883).

=== Build ===

To be able to build the simple-dev.html and app.js files need to point
to the local Ext JS installation: 

 - edit simple-dev.html, comment out the script tag that points to
   "http://cdn.sencha.com/ext/gpl/4.2.1/ext-debug.js", and uncomment
   the one that points to "ext-4.2.1.883/ext-debug.js".

 - edit app.js and in the Loader config replace
   "http://cdn.sencha.com/ext/gpl/4.2.1/src" by "ext-4.2.1.883/src".

You can now update (or create) the build profile:

  /opt/SenchaSDKTools-2.0.0-beta3/sencha create jsb -a simple-dev.html -p simple.jsb3

If the command runs forever it means that simple-dev.html or app.js still
reference cdn.sencha.io.

The simple.jsb3 file is used as input and output, if it doesn't exist a new
file will be created.

Now build the application:

  /opt/SenchaSDKTools-2.0.0-beta3/sencha build -d . -p simple.jsb3

Note that the build includes Ext JS, GeoExt and the application code.
OpenLayers is not included in the build.
