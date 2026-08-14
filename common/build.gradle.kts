plugins {
    id("fabric-loom")
}

val mcVersion = project.property("minecraft_version") as String

base {
    archivesName.set("${project.property("mod_id")}-common")
}

dependencies {
    minecraft("com.mojang:minecraft:$mcVersion")
    compileOnly("net.fabricmc:fabric-loader:${project.property("fabric_loader_version")}")
    compileOnly("net.fabricmc:sponge-mixin:0.17.3+mixin.0.8.7")
}

loom {
    runs {
        remove(getByName("client"))
        remove(getByName("server"))
    }
}

tasks.processResources {
    val modVersion = project.version.toString()
    inputs.property("version", modVersion)
    filesMatching("chestprofiles.mixins.json") {
        expand("version" to modVersion)
    }
}
