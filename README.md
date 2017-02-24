[ ![Download](https://api.bintray.com/packages/cogswell-io/maven/cogs-java-client-sdk/images/download.svg) ](https://bintray.com/cogswell-io/maven/cogs-java-client-sdk/_latestVersion)

# Cogswell Java SDK

The information below is general information provided on using the Cogswell
Java SDK. For code examples and API references, see the corresponding docs.

* For Cogswell Pub/Sub: [Cogswell Pub/Sub Code Samples](API-PUBSUB.md)
* For Cogswell CEP: [Cogswell CEP Code Samples](API-CEP.md)

## Import into Project

The Cogswell Java SDK can be imported into a project either using the .jar files provided, or by including the appropriate information in your build file.

### Using IntelliJ

Follow the instructions under Compile and Install the Source and include the
following information in your `build.gradle` file. Make sure to choose `Use
default gradle wrapper (recommended)` when creating your Gradle project.

```gradle
apply plugin: 'java'

sourceCompatibility = 1.8

repositories {
    mavenLocal()
    jcenter()
}

dependencies {
    compile group: 'io.cogswell', name: 'cogs-java-client-sdk', version: '2.0.0'
}
```

## Compile and Install the Source

Run the following command under the `cogs-java-client-sdk` directory.

### On Linux:

```bash
./gradlew install
```

### On Windows:

```batch
gradlew.bat install
```


