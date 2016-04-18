[ ![Download](https://api.bintray.com/packages/cogswell-io/maven/cogs-java-client-sdk/images/download.svg) ](https://bintray.com/cogswell-io/maven/cogs-java-client-sdk/_latestVersion)

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
Future<GambitResponse> future = cogsService.sendEvent(eventBuilder);

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

// The primary key attributes for identifyig the topic to which we are
// subscribing. The names and types of each attribute should match the
// namespace schema.
LinkedHashMap<String, Object> attributes;

GambitPushService.Builder pushBuilder;

// Assemble the push service definition, but do not build it.
pushBuilder = new GambitPushService.Builder(accessKey, clientSalt, clientSecret)
    .setNamespace(namespace)
    .setAttributes(attributes);

// Start the new push service (WebSocket)
cogsService.startPushService(pushBuilder);
```

