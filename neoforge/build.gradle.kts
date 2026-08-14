plugins {
    id("fabric-loom")
}

val mcVersion = project.property("minecraft_version") as String

base {
    archivesName.set("${project.property("mod_id")}-neoforge")
}

dependencies {
    minecraft("com.mojang:minecraft:$mcVersion")
    compileOnly("net.neoforged:neoforge:26.2.0.59:universal")
    compileOnly("net.neoforged.fancymodloader:loader:11.0.16")
    compileOnly("net.neoforged:bus:8.0.5")

    implementation(project(":common"))
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("META-INF/neoforge.mods.toml") {
        expand("version" to project.version)
    }
    from(project(":common").sourceSets.main.get().resources)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.jar {
    archiveBaseName.set("${project.property("mod_id")}-neoforge")
    archiveVersion.set("${project.version}-mc$mcVersion")
    archiveClassifier.set("")
    from(project(":common").sourceSets.main.get().output)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
