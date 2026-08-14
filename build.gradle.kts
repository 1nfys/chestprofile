plugins {
    id("fabric-loom") version "1.17-SNAPSHOT" apply false
    id("java")
}

val mcVersion = project.property("minecraft_version") as String

subprojects {
    apply(plugin = "java")

    version = project.property("mod_version") as String
    group = project.property("mod_group") as String

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(25))
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.release.set(25)
        options.encoding = "UTF-8"
    }

    tasks.withType<Copy>().configureEach {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    tasks.withType<Jar>().configureEach {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    repositories {
        maven("https://maven.fabricmc.net/") { name = "Fabric" }
        maven("https://maven.neoforged.net/releases/") { name = "NeoForge" }
        mavenCentral()
    }
}

tasks.jar {
    enabled = false
}

val collectJars = tasks.register<Copy>("collectJars") {
    into(layout.buildDirectory.dir("libs"))
    from(project(":fabric").layout.buildDirectory.dir("libs")) {
        include("*.jar")
    }
    from(project(":neoforge").layout.buildDirectory.dir("libs")) {
        include("*.jar")
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.build {
    dependsOn(":fabric:build", ":neoforge:build")
    finalizedBy(collectJars)
}
