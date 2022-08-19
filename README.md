[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)

![badge-Android](https://img.shields.io/badge/Platform-Android-brightgreen)
![badge-iOS](https://img.shields.io/badge/Platform-iOS-lightgray)
![badge-JVM](https://img.shields.io/badge/Platform-JVM-orange)

# KaffeeVerde

This is a set of support libraries that makes porting of Jetpack Compose apps easier to KMP.

This library **IS NOT READY FOR PRODUCTION USE**.

## Setup

These libraries are not available through popular repositories. You can build and publish them into your local Maven repository:
- Clone this repo using 'git clone'
- Run the publishToMavenLocal task from the root of the project:
```
./gradlew publishToMavenLocal
```
- Add dependencies **in your common module's commonMain sourceSet** as follow:
```
api("net.smarttuner.kaffeeverde:core:kaffeeverde_core_version")
```
and so on

## Credits

The source code of this repository was made possible thanks to these works:
- Android Open Source Project - AOSP: https://android.googlesource.com/
- GNU Classpath: https://developer.classpath.org/
- OpenJDK : https://openjdk.org/
- PreCompose : https://github.com/Tlaster/PreCompose

## License

``` 
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
