plugins {
    kotlin("jvm") version "2.2.21"
}

group = "com.rixafy"
version = "1.0.0"

repositories {
    mavenCentral()
    maven {
        url = uri("https://repo.hypixel.net/repository/Hytale/")
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

dependencies {
    compileOnly(files("libraries/HytaleServer.jar"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.json:json:20231013")
}

tasks.jar {
    archiveFileName.set("VoteRewards-${version}.jar")

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(sourceSets.main.get().output)

    from(configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }) {
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
    }

    from("src/main/resources") {
        include("manifest.json")
    }
}
