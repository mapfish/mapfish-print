The docs module is responsible for generating a documentation website.

To build the docs run:

> ./gradlew buildDocs

The output (IE the generated site) is in the folder docs/build/site.

The documentation is compiled from several sources:

-   docssrcmaingroovyGenerateDocs.groovy loads the spring application context and loads all the beans. Using
    these beans it generates parses the Javadocs to find the documentation for all the attributes, processor,
    output formats, etc... The documentation is placed in the docs/build/site/strings-en.json file. This is
    done so we can later provide other translations.
-   The site is AngularJS and Angular-Bootstrap and the source files are in docs/src/resources
-   The Javadocs are also copied into the site and will be linked to from the main documentation.

docssrcmaingroovyGenerateDocs.groovy does some magic when generating the final strings-en.json file that is
found in docs/build/site. It creates it from three sources:

-   The start of the file is copied from docs/src/resources/strings-en.json.
-   The next part of the file is all the text from docs/src/resources/long-strings. These files allow one to
    write long sections of html in a format that is easier to write (because JSON has to be a single line).
    These files are taken by GenerateDocs and escaped correctly so that it is valid json and the result is
    written to docs/build/site/strings-en.json.
-   The last part is from the parsing of the attributes, processor, output formats, etc... beans as mentioned
    above.

Updating the online documentation
=================================

The [online documentation](https://mapfish.github.io/mapfish-print-doc/) lives in the branch gh-pages of the
repository [<https://github.com/mapfish/mapfish-print-doc>](https://github.com/mapfish/mapfish-print-doc).
After having build the documentation with ./gradlew buildDocs the repository can be updated with the following
commands:

    cd mapfish-print-doc
    git fetch origin
    git checkout gh-pages
    git merge --ff-only origin/gh-pages
    git rm --ignore-unmatch -rqf .
    cp -r ../mapfish-print/docs/build/site/. .
    git add -A .
    git commit -m 'Update docs'
    git push origin gh-pages
