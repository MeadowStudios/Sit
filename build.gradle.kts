plugins {
    java
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.18"
}

group = "me.meadow"
version = "1.3"

dependencies {
    paperweight.paperDevBundle(providers.gradleProperty("paperDevBundle").get())
    compileOnly("me.clip:placeholderapi:2.11.6")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(21)
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

tasks.named("build") {
    dependsOn("reobfJar")
}
