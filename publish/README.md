Summary
=======

This module is responsible for deploying the project to the maven repositories. At the time of this writing it
uploads several artifacts to the [GitHub
Packages](https://help.github.com/en/github/managing-packages-with-github-packages/configuring-apache-maven-for-use-with-github-packages)
which is the staging repository for maven central.

Automated Process
=================

1.  Edit the root `build.gradle` to change the `allprojects/version` and commit that with a
    `release/${version}` tag.
2.  Push the commit and the tag to GitHub.

Docker daily build
------------------

1.  Create a branch for the new version `x.y`.
