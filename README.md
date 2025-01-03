# Elytra Gradle Plugin

[![](https://jitpack.io/v/ElytraServers/elytra-gradle.svg?style=flat-square)](https://jitpack.io/#ElytraServers/elytra-gradle)

This plugin is designed to be a helpful tool for developing legacy Minecraft 1.7.10 Mods, where developers usually put
their localization texts in the code, and generate the .lang files by custom Gradle tasks.

## Usage

Well, JitPack is somehow compatible with Gradle Plugin projects.

In `build.gradle[.kts]`

```groovy
plugins {
    id "com.github.ElytraServers.elytra-gradle" version "commit-hash-or-branch-name-or-release-version"
}
```

In `settings.gradle[.kts]`

```groovy
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        mavenLocal()
        // ... other maven repositories for plugins

        maven {
            url = "https://jitpack.io"
        }
    }
}
```

<details>
<summary>Outdated</summary>

Since I don't hold a Maven repository, so you'll need to add this plugin via JitPack in a pretty dirty way.

In `build.gradle` or `build.gradle.kts`

```groovy
plugins {
    id "cn.elytra.gradle" version "x.y.z"
}
```

In `settings.gradle` or `settings.gradle.kts`

```groovy
pluginManagement {
    repositories {
        gradlePluginPortal()
        maven {
            url 'https://jitpack.io'
        }
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "cn.elytra.gradle") {
                useModule('com.github.ElytraServers:elytra-gradle:[commit-name-or-branch-or-release-version]')
            }
        }
    }
}
```

Or, you can clone, compile and publish to your local Maven repository using task `publishToMavenLocal`.

</details>

### Tasks

#### `generateLanguageFiles`

This task will collect the comments with special patterns in the source code files, and generate language files to the
output directory.

You need to add source files via `addSourceDirectorySet(SourceDirectorySet)`.

The default localization key pattern is `//#tr <key>`, and the localization value pattern is
`// <lang_code> <translated_text>`. The whitespaces are sensitive, so keep them.

The key pattern and the value patterns should be together without an empty line in-between.

You also need to add your language codes to the task via `allowedLanguageCodes`, if not, only English (en_US) is
considered as localization text.\
The language code should be equal to the language filename. For example, if you are
adding Simp. Chinese, the language file is `zh_CN.lang`, so the language code is `zh_CN`.

As for existing projects with different patterns, you can set the key pattern via `keyPattern` and the value pattern
via `keyPattern`. They are both used to compile into _Pattern_ for matching.

```groovy
generateLanguageFiles {
    addSourceDirectorySet(sourceSets.main.java)
    addSourceDirectorySet(sourceSets.main.kotlin) // if you have kotlin source set

    allowedLanguageCodes = ["en_US", "zh_CN"] // set the allowed language codes, en_US is only default
    setOutputDirectory(file("testGenerated")) // the output dir of language files, commonly set to /src/resources/assets/{modid}/lang/
}

```

See [TestProject/build.gradle](/TestProject/build.gradle).

## Credit

