plugins {
    id("java")
}

group = "com.github.pmh75"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.momirealms.net/releases/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    compileOnly("net.momirealms:craft-engine-core:26.6.2")
    compileOnly("net.momirealms:craft-engine-bukkit:26.6.2")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.withType<Javadoc> {
    options.encoding = "UTF-8"
}
