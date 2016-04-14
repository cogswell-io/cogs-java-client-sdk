[ ![Download](https://api.bintray.com/packages/cogswell-io/maven/cogs-java-client-sdk/images/download.svg) ](https://bintray.com/cogswell-io/maven/cogs-java-client-sdk/_latestVersion)

## Compile and install the source

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

## [Code samples for the Java Client SDK](#code-samples)

### Preparation for using the Client SDK

```java
import com.gambit.sdk.GambitSDKService;

// Hex encoded access-key from one of your api keys in the Web UI.
String accessKey;

// Hex encoded client salt/secret pair acquired from /client_secret endpoint and
// associated with above access-key.
String clientSalt;
String clientSecret;

GambitSDKService cogsService = GambitSDKService.getInstance();
```

### POST /event
This API route is used send an event to Cogs.

```java
import java.util.concurrent.Future;
import com.gambit.sdk.request.GambitRequestEvent.Builder;

// This should contain the current time in ISO-8601 format.
String timestamp;

// The name of the namespace for which the event is destined.
String namespace

// This will be sent along with messages so that you can identify the event which
// "triggered" the message delivery.
String eventName;

// The optional ID of the campaign to which this event is responsing. This can
// either be omitted or set to -1 for no campaign.
Integer campaignId;

// The event attributes whose names and types should match the namespace schema.
LinkedHashMap<String, Object> attributes;

// Assemble the event, but do not build it yet.
Builer eventBuilder = new Builder(accessKey, clientSalt, clientSecret);
    .setTimestamp(timestamp)
    .setEventName(eventName)
    .setCampaignId(campaignId)
    .setNamespace(namespace)
    .setAttributes(attributes;

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

// Shutdown the service when you are done using it.
cogsService.finish();
```

### GET /push
This API route is used to establish a push WebSocket.


```java
```

