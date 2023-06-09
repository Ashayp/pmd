# How to build PMD

PMD uses [Maven](https://maven.apache.org/) and requires at least Java 11 for building.
You can get Java 11 from [Oracle](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
or from [AdoptOpenJdk](https://adoptopenjdk.net/).

PMD uses the [maven wrapper](https://maven.apache.org/wrapper/), so you can simply build PMD as following:

*   `./mvnw clean verify` (on Unix-like platform such as Linux and Mac OS X)
*   `mvnw.cmd clean verify` (on Windows)

This will create the zip files in the directory `pmd-dist/target`:

    cd pmd-dist/target
    ls *.zip

That's all !

**Note:** While Java 11 is required for building, running PMD only requires Java 7
(or Java 8 for Apex, JavaScript, Scala, Visualforce, and the Designer).

**Note:** With PMD 6.24.0, we are creating [Reproducible Builds](https://reproducible-builds.org/). Since we use
[Maven](https://maven.apache.org/guides/mini/guide-reproducible-builds.html) for building, the following
limitations apply:

*   Generally give **different results on Windows and Unix** because of different newlines.
    (carriage return linefeed on Windows, linefeed on Unixes).
    
    We build our releases under **Linux** on [Github Actions](https://github.com/pmd/pmd/actions).

*   Generally depend on the **major version of the JDK** used to compile. (Even with source/target defined,
    each major JDK version changes the generated bytecode.).
    
    We build our releases using OpenJDK 11.

## How to build the documentation?

    cd docs
    bundle install # once
    bundle exec jekyll build

You'll find the built site in the directory `_site/`.

For more info, see [README in docs directory](docs/README.md).
