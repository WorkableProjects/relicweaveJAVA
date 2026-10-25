# Getting the RelicWeave JAR

The mod JAR is not stored in the repository. You build it from the source code with the included Gradle wrapper. It takes a few minutes the first time, because Gradle downloads Minecraft and Forge.

## 1. Install Java 17

Install a **JDK 17** (for example [Eclipse Temurin 17](https://adoptium.net/temurin/releases/?version=17)). A newer JDK also works for running Gradle; the build compiles with Java 17 either way and downloads it if it's missing.

Check it in a terminal:

```
java -version
```

## 2. Get the source

Either clone it:

```
git clone https://github.com/WorkableProjects/relicweaveJAVA.git
cd relicweaveJAVA
git checkout claude/compassionate-lamport-4irs1x
```

or on GitHub choose the branch, then **Code → Download ZIP**, and unzip it.

## 3. Build

From the project folder:

- **Windows:** `gradlew.bat build`
- **macOS / Linux:** `./gradlew build`

Wait for `BUILD SUCCESSFUL`.

## 4. Find the JAR

The finished mod is at:

```
build/libs/relicweave-0.0.2.jar
```

That file is ready to upload to CurseForge or to install. The file name follows `mod_version` in `gradle.properties`.

## 5. Install it (to test)

1. Install **Forge 47.3.0 or newer** for **Minecraft 1.20.1**.
2. Copy `relicweave-0.0.2.jar` into the `mods` folder of your Minecraft instance (and of your server, for multiplayer).
3. Start Minecraft with the Forge profile.

## Troubleshooting

- **`JAVA_HOME is not set` / `java not found`:** Java isn't installed or isn't on your PATH. Reinstall the JDK and tick "Set JAVA_HOME" in the installer.
- **Download errors or timeouts:** run the build again. Gradle resumes where it stopped.
- **Old jars in `build/libs`:** only use the one whose version matches `gradle.properties`. Run `gradlew clean build` to start fresh.
