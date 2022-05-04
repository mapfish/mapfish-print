# Summary

This module is responsible for deploying the project to the maven repositories. At the time of this writing it
uploads several artifacts to the [GitHub
Packages](https://help.github.com/en/github/managing-packages-with-github-packages/configuring-apache-maven-for-use-with-github-packages)
which is the staging repository for maven central.

# Automated Process

1.  Create a `x.y.z` tag.

## Docker daily build

1.  Create a branch for the new version `x.y`.
