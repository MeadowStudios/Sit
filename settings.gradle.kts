pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.helpch.at/releases")
        mavenCentral()
    }
}

rootProject.name = "Sit"
