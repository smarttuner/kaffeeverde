[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)

![badge-Android](https://img.shields.io/badge/Platform-Android-brightgreen)
![badge-iOS](https://img.shields.io/badge/Platform-iOS-lightgray)
![badge-JVM](https://img.shields.io/badge/Platform-JVM-orange)

# KaffeeVerde-Core

This is a support library that makes porting of some AndroidX APIs easier to KMP.

This library **IS NOT READY FOR PRODUCTION USE**.

## Setup

This library is not available through popular repositories. You can build and publish it into your local Maven repository:
- Clone this repo using 'git clone'
- Run the publishToMavenLocal task from the root of the project:
```
./gradlew publishToMavenLocal
```
- Add the dependency **in your common module's commonMain sourceSet**:
```
api("net.smarttuner.kaffeeverde:core:kaffeeverde_core_version")
```