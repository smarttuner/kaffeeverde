const val androidPlugin = "android"
const val cocoapods = "native.cocoapods"
const val androidLib = "com.android.library"
const val multiplatform = "multiplatform"
const val mavenPublish = "maven-publish"


object Versions {
    const val ANDROID_MIN_SDK = 24
    const val ANDROID_TARGET_SDK = 32
    const val ANDROID_COMPILE_SDK = 32

    const val KOTLIN = "1.7.0"
    const val KOTLIN_GRADLE_PLUGIN = KOTLIN
    const val ANDROID_GRADLE_PLUGIN = "7.2.1"
    const val NAPIER = "2.6.1"

    const val KTOR = "2.0.1"
    const val UUID = "0.4.0"
    const val DITCHOOM_BUFFER = "1.0.86"
}

object Deps {
    const val android_gradle_plugin = "com.android.tools.build:gradle:${Versions.ANDROID_GRADLE_PLUGIN}"
    const val kotlin_gradle_plugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.KOTLIN_GRADLE_PLUGIN}"

    object Napier{
        const val Napier = "io.github.aakira:napier:${Versions.NAPIER}"
    }
    object Ktor {
        const val Core = "io.ktor:ktor-client-core:${Versions.KTOR}"
    }
    object UUID {
        const val UUID = "com.benasher44:uuid:${Versions.UUID}"
    }
    object DitchoomBuffer {
        const val DitchoomBuffer = "com.ditchoom:buffer:${Versions.DITCHOOM_BUFFER}"
    }
}
