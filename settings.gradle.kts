pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/") { name = "Fabric" }
        maven("https://maven.neoforged.net/releases/") { name = "NeoForge" }
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "chestprofiles"

include("common")
include("fabric")
include("neoforge")
