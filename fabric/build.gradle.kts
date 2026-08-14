plugins {
    id("fabric-loom")
}

val mcVersion = project.property("minecraft_version") as String

base {
    archivesName.set("${project.property("mod_id")}-fabric")
}

dependencies {
    minecraft("com.mojang:minecraft:$mcVersion")
    implementation("net.fabricmc:fabric-loader:${project.property("fabric_loader_version")}")
    implementation("net.fabricmc.fabric-api:fabric-api:${project.property("fabric_api_version")}")
    implementation("net.fabricmc.fabric-api:fabric-key-mapping-api-v1:2.0.5+e2bdee789e")

    implementation(project(":common"))
}

tasks.processResources {
    val modVersion = project.version.toString()
    inputs.property("version", modVersion)
    filesMatching("fabric.mod.json") {
        expand("version" to modVersion)
    }
    from(project(":common").sourceSets.main.get().resources)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.jar {
    archiveBaseName.set("${project.property("mod_id")}-fabric")
    archiveVersion.set("${project.version}-mc$mcVersion")
    archiveClassifier.set("")
    from(project(":common").sourceSets.main.get().output)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
