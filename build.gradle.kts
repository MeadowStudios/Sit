plugins {
    java
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.20"
}

group = "me.meadow"
version = "1.5"

dependencies {
    paperweight.paperDevBundle(providers.gradleProperty("paperDevBundle").get())
    compileOnly("me.clip:placeholderapi:2.11.6")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(25)
        options.isDebug = false
    }

    processResources {
        filteringCharset = "UTF-8"
        filesMatching("plugin.yml") {
            expand("version" to project.version)
        }
    }

    jar {
        archiveBaseName.set("Sit")
        archiveVersion.set(project.version.toString())
    }
}