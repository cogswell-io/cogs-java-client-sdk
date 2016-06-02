[ ![Download](https://api.bintray.com/packages/cogswell-io/maven/cogs-java-client-sdk/images/download.svg) ](https://bintray.com/cogswell-io/maven/cogs-java-client-sdk/_latestVersion)

## [Import into Project] (#import-into-project)
The Java SDK can either be imported from the .jar files provided or by including the following in a Gradle build file
```
dependencies {
    compile 'io.cogswell:cogs-java-client-sdk:1.0.+'
}
```
## [Compile and install the source] (#build-details)

Linux:
```
./gradlew install
```

Windows:
```
gradlew.bat install
```

## IntelliJ IDEA

If you open this in IntelliJ IDEA, in the "Import Project from Gradle" dialog, select "Use customizable gradle wrapper".

## [Code Samples](#code-samples)
You will see the name Gambit throughout our code samples. This was the code name used for Cogs prior to release.

### Preparation for using the Client SDK

The Client SDK uses a client secret and salt generated from the access key you generate at the cogswell.io web site.  You can create the secret and salt in either of two ways:
* Use the Java Test Tool, available at https://cogswell.io/#!/api-docs
* Use the API directly: https://cogswell.io/docs/api/#!/Client_secret/post_client_secret

```java
import com.gambit.sdk.GambitSDKService;

// Hex encoded access-key from one of your api keys in the Web UI.
String accessKey;

// Hex encoded client salt/secret pair acquired from /client_secret endpoint and
// associated with above access-key.
String clientSalt;
String clientSecret;

// Create and setup the Cogs SDK service
GambitSDKService cogsService = GambitSDKService.getInstance();

// Shutdown the service when you are done using it (when your program closes).
// You can add this to a shutdown hook if you'd like. It closes all push service
// WebSockets for you so you don't need to do so explicitly.
cogsService.finish();
```

### POST /event
This API route is used send an event to Cogs.

```java
import java.util.LinkedHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.CancelledException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import com.gambit.sdk.GambitResponse;
import com.gambit.sdk.request.GambitRequestEvent;

// This should contain the current time in ISO-8601 format.
String timestamp;

// The name of the namespace for which the event is destined.
String namespace;

// This will be sent along with messages so that you can identify the event which
// "triggered" the message delivery.
String eventName;

// The optional ID of the campaign to which this event is responsing. This can
// either be omitted or set to -1 for no campaign.
Integer campaignId;

// The event attributes whose names and types should match the namespace schema.
LinkedHashMap<String, Object> attributes;

GambitRequestEvent.Builder eventBuilder;

// Assemble the event, but do not build it.
eventBuilder = new GambitRequestEvent.Builder(accessKey, clientSalt, clientSecret)
    .setTimestamp(timestamp)
    .setEventName(eventName)
    .setCampaignId(campaignId)
    .setNamespace(namespace)
    .setAttributes(attributes);

// Send the event, and receive a Future through which you can handle the outcome
// of the event delivery attempt.
Future<GambitResponse> future = cogsService.sendGambitEvent(eventBuilder);

// In this example we are simply blocking until the operation completes, timing
// out after 15 seconds. If you do no wish to block the calling thread, you will
// need to either poll for completion (future.isDone()) or block for completion
// in another thread (Callable in executor service or explicitly managed thread).
try {
  GambitResponse response = future.get(15, TimeUnit.SECONDS);
} catch (CancelledException eCanceled) {
  // Handle cancellation
} catch (InterruptedException eInterrupted) {
  // Handle interruption
} catch (ExecutionException eExecution) {
  // Handle execution error
}
```

### GET /push
This API route is used to establish a push WebSocket.

```java
import java.util.LinkedHashMap;
import com.gambit.sdk.GambitPushService;

GambitPushService.GambitMessageListener messageListener;

// Create the message listener, which handles all incoming messages.
messageListener = (builder, message) -> System.out.println("Message: " + message);

// Add the message listener to the Cogs service.
cogsService.setGambitMessageListener(messageListener);

// The name of the namespace to which the new push service will be bound.
String namespace;

// A description of the topic.
String topicDescription;

// The primary key attributes for identifyig the topic to which we are
// subscribing. The names and types of each attribute should match the
// namespace schema.
LinkedHashMap<String, Object> attributes;

GambitPushService.Builder pushBuilder;

// Assemble the push service definition, but do not build it.
pushBuilder = new GambitPushService.Builder(accessKey, clientSalt, clientSecret, topicDescription)
    .setNamespace(namespace)
    .setAttributes(attributes);

// Start the new push service (WebSocket)
cogsService.startPushService(pushBuilder);
```

## [Complete Example] (#complete-example)
If you have already completed a step, like creating a cogswell.io account, you may skip it.
1. Open https://cogswell.io, create and login to an account.
2. Create and save a namespace called "TestNamespace" with a primary key attribute "email".
3. Create and save a campaign with "Notification Message" set to "test message" and "Show to:  Whomever triggers this campaign" selected (it will appear as a blue-bordered square when selected.)
4. Create a client secret and salt (Setup > Client Keys)
5. Create a new directory "CogsJavaExample" for this project.
6. Install gradle or gradlew:
  1. gradle: Follow https://docs.gradle.org/current/userguide/installation.html
    1. Make sure you have Java 1.8 or later installed.
  2. gradlew: Copy gradle/* (including all subdirectories and files,) gradlew (for Linux and OSX,) and gradlew.bat (for windows,) from this github repo into your new "CogsJavaExample" directory
6. Add the files below ("CogsJavaExample/build.gradle" and "CogsJavaExample/src/main/java/HelloCogs.java")
7. Your files should now have the following structure.  If you are not using gradlew, you will not have the gradle files.
  1. CogsJavaExample/
    1. build.gradle
    2. gradle/
      1. wrapper
        1. gradle-wrapper.jar
        2. gradle-wrapper.properties
    3. gradlew
    4. gradlew.bat
    5. src/
      1. main/
        1. java/
          1. HelloCogs.java
8. Update the namespace, accessKey, clientSecret, clientSalt in HelloCogs.java
9. Run it: linux/OSX "./gradlew run", Windows "gradlew.bat run"
10. If you get a build error "Unable to locate tools.jar" or you get version or build errors, you may not have your JAVA_HOME set.
  1. Find the path to java jdk 1.8.  You must use Java 1.8 or later.
    1. /usr/libexec/java_home -v 1.8
  2. set the path.  For example:
    1. export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_91.jdk/Contents/Home
  3. Re-run the gradle command.

*`CogsJavaExample/build.gradle`*
```
apply plugin: 'application'
repositories {
    jcenter()
}
dependencies {
    compile 'io.cogswell:cogs-java-client-sdk:1.0.+'
}
mainClassName = "HelloCogs"
```
*`CogsJavaExample/src/main/java/HelloCogs.java`*
```
import java.util.*;
import java.text.*;
import java.util.concurrent.*;
import com.gambit.sdk.*;
import com.gambit.sdk.request.GambitRequestEvent;
import com.gambit.sdk.response.GambitResponseEvent;

class HelloCogs {
    public static void main(String args[]) {
        System.out.println("Starting...");
        HelloCogs c = new HelloCogs();

        // The primary key attributes for identifyig the topic to which we are
        // subscribing. The names and types of each attribute should match the
        // namespace schema.
        LinkedHashMap<String, Object> subscriptionAttributes;
        subscriptionAttributes = new  LinkedHashMap<String, Object>();
        subscriptionAttributes.put("email", "abc");

        c.subscribe(subscriptionAttributes);

        // The event attributes whose names and types should match the namespace schema.
        LinkedHashMap<String, Object> eventAttributes;
        eventAttributes = new  LinkedHashMap<String, Object>();
        eventAttributes.put("email", "abc");

        c.sendEvent(eventAttributes);
    }
    public HelloCogs() {
        init();
    }

    // Hex encoded access-key from one of your api keys in the Web UI.
    String accessKey;

    // Hex encoded client salt/secret pair acquired from /client_secret endpoint and
    // associated with above access-key.
    String clientSecret;
    String clientSalt;

    GambitSDKService cogsService;

    private void init() {
        System.out.println("init()");

        // Update these to use your values.
        namespace = "TestNamespace";
        accessKey = "********";
        clientSecret = "********";
        clientSalt = "********";

        // Create and setup the Cogs SDK service
        cogsService = GambitSDKService.getInstance();
    }
    public void finish() {
        // Shutdown the service when you are done using it (when your program closes).
        // You can add this to a shutdown hook if you'd like. It closes all push service
        // WebSockets for you so you don't need to do so explicitly.
        cogsService.finish();
    }

    // This should contain the current time in ISO-8601 format.
    String timestamp;

    // The name of the namespace for which the event is destined.
    String namespace;

    // This will be sent along with messages so that you can identify the event which
    // "triggered" the message delivery.
    String eventName;

    // The optional ID of the campaign to which this event is responsing. This can
    // either be omitted or set to -1 for no campaign.
    Integer campaignId;

    GambitRequestEvent.Builder eventBuilder;
    public void sendEvent(LinkedHashMap<String, Object> attributes) {
        System.out.println("sendEvent()");

        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
        df.setTimeZone(tz);
        timestamp = df.format(new Date());

        eventName = "example_event_name";
        campaignId = -1;

        // Assemble the event, but do not build it.
        eventBuilder = new GambitRequestEvent.Builder(accessKey, clientSalt, clientSecret)
                .setTimestamp(timestamp)
                .setEventName(eventName)
                .setCampaignId(campaignId)
                .setNamespace(namespace)
                .setAttributes(attributes);

        try {
            // Send the event, and receive a Future through which you can handle the outcome
            // of the event delivery attempt.
            Future<GambitResponse> future = cogsService.sendGambitEvent(eventBuilder);

            // In this example we are simply blocking until the operation completes, timing
            // out after 15 seconds. If you do no wish to block the calling thread, you will
            // need to either poll for completion (future.isDone()) or block for completion
            // in another thread (Callable in executor service or explicitly managed thread).
            //
            GambitResponseEvent response = (GambitResponseEvent) future.get(15, TimeUnit.SECONDS);
            System.out.println("sendEvent() GambitResponse: "+response.getRawBody());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    GambitPushService.GambitMessageListener messageListener;

    public void subscribe(LinkedHashMap<String, Object> attributes) {

        // Create the message listener, which handles all incoming messages.
        messageListener = (builder, message) -> System.out.println("Message: " + message.getRawMessage());

        // Add the message listener to the Cogs service.
        cogsService.setGambitMessageListener(messageListener);

        // A description of the topic.
        String topicDescription = "topicDescription";

        GambitPushService.Builder pushBuilder;

        // Assemble the push service definition, but do not build it.
        pushBuilder = new GambitPushService.Builder(accessKey, clientSalt, clientSecret, topicDescription)
                .setNamespace(namespace)
                .setAttributes(attributes);

        // Start the new push service (WebSocket)
        try {
            cogsService.startPushService(pushBuilder);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```
