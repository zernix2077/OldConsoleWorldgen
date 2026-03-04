plugins {
    id("java-library")
    id("org.allaymc.gradle.plugin") version "0.2.1"
}

group = "xyz.zernix.oldconsoleworldgen"
description = "Bring back minecraft legacy console edition world generation."
version = "0.1.1"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

allay {
    api = "0.25.0"

    plugin {
        entrance = ".OldConsoleWorldGenPlugin"
        authors += "Zernix"
    }
}

dependencies {
    compileOnly(group = "org.projectlombok", name = "lombok", version = "1.18.34")
    annotationProcessor(group = "org.projectlombok", name = "lombok", version = "1.18.34")
}
