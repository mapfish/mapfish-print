# [![GeoExt Logo](http://geoext.github.io/geoext2/website-resources/img/GeoExt-logo.png "GeoExt Logo")](http://geoext.github.io/geoext2/) [GeoExt 2](http://geoext.github.io/geoext2/)

JavaScript Toolkit for Rich Web Mapping Applications.

[![Build Status](https://travis-ci.org/geoext/geoext2.png)](https://travis-ci.org/geoext/geoext2)


## What is GeoExt?

GeoExt is Open Source and enables building desktop-like GIS applications through
the web. It is a JavaScript framework that combines the GIS functionality of
[OpenLayers](http://openlayers.org) with the user interface savvy of the
[ExtJS](http://www.sencha.com/products/extjs/) library provided by
[Sencha](http://www.sencha.com/).

Version 2 of GeoExt is the successor to the
[GeoExt 1.x-series](http://geoext.org) and is built atop the newest official
installments of its base libraries; OpenLayers 2.13.1 and ExtJS 4.2.1.


## More information on GeoExt 2

Have a look at the official homepage: http://geoext.github.io/geoext2/

You will find examples, API documentation (with and without inherited
functionality from ExtJS), links to mailinglists and more over there.

What you see on http://geoext.github.io/geoext2/ are the contents of the
`gh-pages`-branch. If you encounter anything that should be fixed, please issue
a pull request against that branch and we will merge it as soon as possible.


## Ok, I want to use GeoExt now...

Hey that's a good decision, and it is very easy to get started on a page that
already includes ExtJS and OpenLayers:

```html
<!DOCTYPE html>
<html>
    <head>
        <title>Trying out GeoExt2</title>
        <!-- Load the ExtJS stylesheet -->
        <link rel="stylesheet" type="text/css"
          href="http://cdn.sencha.com/ext/gpl/4.2.1/resources/css/ext-all.css">
        <!-- Load ExtJS from their CDN, local versions work also -->
        <script type="text/javascript"
          src="http://cdn.sencha.com/ext/gpl/4.2.1/ext-debug.js"></script>
        <!-- Load OpenLayers, custom builds may even be better -->
        <script src="http://openlayers.org/api/2.13.1/OpenLayers.js"></script>
        <script type="text/javascript" src="../loader.js"></script>
    </head>
    <body></body>
</html>
```

Next, we simply add a `<script>`-tag in which we tell ExtJS about GeoExt (and
also where to find the ExtJS source files):

```html
<script type="text/javascript">
Ext.Loader.setConfig({
    enabled: true,
    disableCaching: false,
    paths: {
        GeoExt: "path/to/src/GeoExt",
        Ext: "http://cdn.sencha.com/ext/gpl/4.2.1/src"
    }
});
</script>
```

That's it. Now you can e.g. create a very basic mappanel and let it reign over
the whole browser window:

```javascript
var mappanel = Ext.create('GeoExt.panel.Map', {
    title: 'A sample Map',
    map: {
        // ...
        // optional, can be either
        //   - a valid OpenLayers.Map configuration or
        //   - an instance of OpenLayers.Map
    },
    center: '12.31,51.48',
    zoom: 6
});

Ext.create('Ext.container.Viewport', {
    layout: 'fit',
    items: [
        mappanel // our variable from above
    ]
});
```

More information to get started can be grabbed
[on the main website](http://geoext.github.io/geoext2).


## Want to contribute? Yes, please!

We definitely want you to help us making GeoExt. We will happily accept pull
requests of any kind; be it documentation improvement, code refactoring or new
functionality.

Please sign the [contributor agreement](http://trac.geoext.org/browser/docs/contributor_agreements/geoext_agreement.pdf?format=raw)
and email it to the GeoExt Project Steering Committee (`psc [at] geoext.org`)
prior to submitting your changes. Thanks.


## To generate the jsduck documentation

*   Install jsduck: https://github.com/senchalabs/jsduck
*   Run `jsduck --config jsduck.json` in the root of the repository
*   Optional: If you want the documentation for ExtJS to be linked, edit
    `jsduck.json` to point to the proper source location (URLs will not work)
*   Open the generated file `/path/to/your/geoext/docs/index.html` in your
    favorite browser
*   Enjoy!


## To run the test suite

*   Serve the contents of a GeoExt 2 clone on a webserver, e.g.:

    `user@machine:/src/geoext2 $ python -m SimpleHTTPServer 2222`

*   Open the main test suite HTML file in a browser:

    `http://localhost:2222/tests/run-tests.html`

*   Click the button "run all"

## Headless tests with casperjs

You can also run the above tests headlessly through casperjs:

```bash
# Run the suite in the root of the repository
./tests/headless/casperjs-1.1-beta.1/bin/casperjs \
  test \
  tests/headless/run-testsuite.js
```

These tests are also run though [travis](https://travis-ci.org/geoext/geoext2).


