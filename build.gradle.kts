
plugins {
    application
    kotlin("jvm") version "1.3.31"
    distribution
}


application {
    mainClassName = "com.github.phweda.mrf.MameRecordFile";
    group = "com.github.phweda.mrf"
    version = "1.0-SNAPSHOT"
}


repositories {
    mavenCentral()
}


dependencies {
    compile(
        "org.jetbrains.kotlin:kotlin-stdlib:1.3.31"
    )
    implementation(kotlin("stdlib-jdk7"))
    implementation(kotlin("stdlib-jdk8"))
}


version = "1.0"
val projectname = "mrf"

// TODO figure out how to get Dist build to use the fat jar
val fatJar = task("fatJar", type = Jar::class) {
    baseName = "$projectname"
    manifest {
        attributes["Main-Class"] = "com.github.phweda.mrf.MameRecordFile"
        attributes["Implementation-Title"] = "MAME Record Files"
        attributes["Implementation-Version"] = version
    }

    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })
    with(tasks["jar"] as CopySpec)
}

tasks {
    "build" {
        dependsOn(fatJar)
    }

}

// Add plain text readme to dist - includes github URL
distributions {
    getByName("main") {
        baseName = "$projectname"
        contents {
            from("readme.md")
        }
    }
}


// Alternate fat jar code
/*
tasks.register<Jar>("uberJar") {
    archiveClassifier.set("uber")

    from(sourceSets.main.get().output)

    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })
}
*/


// TODO figure out how to inject JAVA_OPTS to distribution run scripts
/*
$DEFAULT_JVM_OPTS="\"JAVA_OPTS\", \"-Xms12m -Xmx24m\""


    "run"(JavaExec::class) {
        environment("JAVA_OPTS", "-Xms12m -Xmx24m")
    }
*/
