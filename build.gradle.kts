import proguard.gradle.ProGuardTask
import java.io.File

plugins {
    java
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.18"
}

group = "me.meadow"
version = "1.1"

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("com.guardsquare:proguard-gradle:7.6.1")
    }
}

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

val obfuscateJarTask = tasks.register<ProGuardTask>("obfuscateJar") {
    group = "build"
    description = "Creates an obfuscated release jar with ProGuard."

    dependsOn("reobfJar")

    outputs.upToDateWhen { false }

    doFirst {
        val libsDir = layout.buildDirectory.dir("libs").get().asFile

        val candidates = listOf(
            File(libsDir, "Sit-${project.version}.jar"),

            File(libsDir, "Sit-${project.version}-reobf.jar"),
            File(libsDir, "Sit-${project.version}.jar.reobf")
        )

        val inputJar = candidates.firstOrNull { it.exists() }
            ?: throw GradleException(
                "Could not find jar to obfuscate. Checked: " +
                        candidates.joinToString(", ") { it.absolutePath }
            )

        val outputJar = File(libsDir, "Sit-${project.version}-obf.jar")
        val mapFile = layout.buildDirectory.file("reports/proguard-sit-map.txt").get().asFile

        outputJar.parentFile.mkdirs()
        mapFile.parentFile.mkdirs()

        if (outputJar.exists() && !outputJar.delete()) {
            throw GradleException("Could not delete old obfuscated jar: ${outputJar.absolutePath}")
        }

        println("ProGuard input jar: ${inputJar.absolutePath}")
        println("ProGuard output jar: ${outputJar.absolutePath}")

        injars(inputJar)
        outjars(outputJar)
        printmapping(mapFile)

        dontshrink()
        dontoptimize()

        keepattributes(
            "RuntimeVisibleAnnotations," +
                    "RuntimeInvisibleAnnotations," +
                    "Signature," +
                    "InnerClasses," +
                    "EnclosingMethod"
        )

        dontwarn()
        ignorewarnings()

        libraryjars("${System.getProperty("java.home")}/jmods/java.base.jmod")

        keep("public class me.meadow.Sit { *; }")

        keep("public class me.meadow.command.PoseCommand { *; }")
        keep("public class me.meadow.command.PlayerSitCommand { *; }")

        keep("public class me.meadow.pose.PoseListener { *; }")

        keep("public class me.meadow.placeholder.SitPlaceholderExpansion { *; }")

        keep("public class me.meadow.nms.v26_1_2.NmsBridge_26_1_2 { *; }")

        keep("public class me.meadow.nms.v26_1_2.SeatEntity { *; }")
        keep("public class me.meadow.nms.v26_1_2.BoxEntity { *; }")
        keep("public class me.meadow.nms.v26_1_2.HeadSeatEntity { *; }")
        keep("public class me.meadow.nms.v26_1_2.CrawlRenderer { *; }")
        keep("public class me.meadow.nms.v26_1_2.PoseRenderer { *; }")

        keep("interface me.meadow.nms.NmsBridge { *; }")
        keep("interface me.meadow.nms.NmsPose { *; }")
        keep("interface me.meadow.nms.NmsCrawl { *; }")

        keepclassmembers("enum * { public static **[] values(); public static ** valueOf(java.lang.String); }")
    }

    doLast {
        val outputJar = layout.buildDirectory.file("libs/Sit-${project.version}-obf.jar").get().asFile

        if (!outputJar.exists()) {
            throw GradleException("ProGuard finished, but the obfuscated jar was not created: ${outputJar.absolutePath}")
        }

        println("Obfuscated jar created: ${outputJar.absolutePath}")
        println("Do not upload build/reports/proguard-sit-map.txt")
    }
}

tasks.named("build") {
    dependsOn("reobfJar")
    dependsOn(obfuscateJarTask)
}
