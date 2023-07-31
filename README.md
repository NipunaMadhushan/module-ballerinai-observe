
Ballerina Observe Internal Library
===================

[![Build](https://github.com/ballerina-platform/module-ballerinai-observe/actions/workflows/build-timestamped-master.yml/badge.svg)](https://github.com/ballerina-platform/module-ballerinai-observe/actions/workflows/build-timestamped-master.yml)
[![Trivy](https://github.com/ballerina-platform/module-ballerinai-observe/actions/workflows/trivy-scan.yml/badge.svg)](https://github.com/ballerina-platform/module-ballerinai-observe/actions/workflows/trivy-scan.yml)
[![GitHub Last Commit](https://img.shields.io/github/last-commit/ballerina-platform/module-ballerinai-observe.svg)](https://github.com/ballerina-platform/module-ballerinai-observe/commits/master)
[![codecov](https://codecov.io/gh/ballerina-platform/module-ballerinai-observe/branch/master/graph/badge.svg)](https://codecov.io/gh/ballerina-platform/module-ballerinai-observe)

This module contains internal configurations and initializations for Ballerina observability. Ballerina supports observability out of the box. You can use [module-ballerina-observe](https://github.com/ballerina-platform/module-ballerina-observe) in your Ballerina project and enable the observability features.
## Build from the source

### Set Up the prerequisites

1. Download and install Java SE Development Kit (JDK) version 17 (from one of the following locations).

    * [Oracle](https://www.oracle.com/java/technologies/downloads/)

    * [OpenJDK](https://adoptopenjdk.net/)

      > **Note:** Set the JAVA_HOME environment variable to the path name of the directory into which you installed JDK.

2. Export your GitHub Personal access token with read package permissions as follows.

        export packageUser=<Username>
        export packagePAT=<Personal access token>

### Build the source

Execute the commands below to build from source.

1. To build the library:
    ```
    ./gradlew clean build
    ```

2. To run the integration tests:
    ```
    ./gradlew clean test
    ```

3. To run a group of tests
    ```
    ./gradlew clean test -Pgroups=<test_group_names>
    ```

4. To build the package without the tests:
    ```
    ./gradlew clean build -x test
    ```

5. To debug the tests:
    ```
    ./gradlew clean test -Pdebug=<port>
    ```

6. To debug with Ballerina language:
    ```
    ./gradlew clean build -PbalJavaDebug=<port>
    ```

7. Publish the generated artifacts to the local Ballerina central repository:
    ```
    ./gradlew clean build -PpublishToLocalCentral=true
    ```

8. Publish the generated artifacts to the Ballerina central repository:
    ```
    ./gradlew clean build -PpublishToCentral=true
    ```

## Contribute to Ballerina

As an open source project, Ballerina welcomes contributions from the community.

For more information, go to the [contribution guidelines](https://github.com/ballerina-platform/ballerina-lang/blob/master/CONTRIBUTING.md).

## Code of conduct

All contributors are encouraged to read the [Ballerina Code of Conduct](https://ballerina.io/code-of-conduct).

## Useful links

* For example demonstrations of the usage, go to [Ballerina By Examples](https://ballerina.io/learn/by-example/).
* Chat live with us via our [Discord server](https://discord.gg/ballerinalang).
* Post all technical questions on Stack Overflow with the [#ballerina](https://stackoverflow.com/questions/tagged/ballerina) tag.
* View the [Ballerina performance test results](https://github.com/ballerina-platform/ballerina-lang/blob/master/performance/benchmarks/summary.md).


