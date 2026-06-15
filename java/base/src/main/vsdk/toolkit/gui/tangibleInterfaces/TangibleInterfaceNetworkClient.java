package vsdk.toolkit.gui.tangibleInterfaces;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import vsdk.toolkit.common.linealAlgebra.Quaterniond;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;

/**
This class connects to a `TangibleInterfaceMarkersDetectorServer` websocket
service (see `cpp/testsuite/Tools/TangibleInterfaceMarkersDetectorServer`),
listens for the JSON array of marker group poses it streams, and notifies
subscribed `TangibleInterfaceListener`s with a `TangibleInterfaceEvent` for
each reported marker group.

Each streamed JSON entry has the form:

<pre>
{"label":"rayCube","position":[x,y,z],"quaternion":[w,x,y,z]}
</pre>

If the service cannot be reached, this is reported on the console and the
client simply stops, without throwing an exception.
*/
public class TangibleInterfaceNetworkClient implements Runnable {

    private static final Pattern GROUP_PATTERN = Pattern.compile("\\{[^{}]*\\}");
    private static final Pattern LABEL_PATTERN = Pattern.compile("\"label\"\\s*:\\s*\"([^\"]*)\"");
    private static final Pattern POSITION_PATTERN = Pattern.compile("\"position\"\\s*:\\s*\\[([^\\]]*)\\]");
    private static final Pattern QUATERNION_PATTERN = Pattern.compile("\"quaternion\"\\s*:\\s*\\[([^\\]]*)\\]");

    private final String serviceUrl;
    private final List<TangibleInterfaceListener> listeners;
    private final StringBuilder incomingMessage;
    private WebSocket webSocket;

    /**
    @param serviceUrl websocket URL of the tangible interface marker
        tracking service, for example `ws://localhost:8090/v1/values`
    */
    public TangibleInterfaceNetworkClient(String serviceUrl) {
        this.serviceUrl = serviceUrl;
        this.listeners = new ArrayList<TangibleInterfaceListener>();
        this.incomingMessage = new StringBuilder();
    }

    /**
    Subscribes a listener to receive `TangibleInterfaceEvent`s as they
    arrive from the tracking service.

    @param listener the listener to subscribe
    */
    public void addListener(TangibleInterfaceListener listener) {
        if ( listener == null ) {
            return;
        }

        synchronized ( listeners ) {
            listeners.add(listener);
        }
    }

    /**
    Unsubscribes a previously added listener.

    @param listener the listener to unsubscribe
    */
    public void removeListener(TangibleInterfaceListener listener) {
        synchronized ( listeners ) {
            listeners.remove(listener);
        }
    }

    /**
    Spawns a separate thread that connects to the tangible interface
    tracking service and listens for incoming pose updates. This method
    returns immediately to its caller; the connection attempt and listening
    loop run on the spawned thread. If the service is not reachable, a
    message is printed to the console and the spawned thread terminates
    without raising an exception.
    */
    public void run() {
        Thread connectionThread = new Thread(this::connectAndListen, "TangibleInterfaceNetworkClient");
        connectionThread.setDaemon(true);
        connectionThread.start();
    }

    /**
    Requests the websocket connection to be closed, if currently open.
    */
    public void disconnect() {
        if ( webSocket != null ) {
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Client disconnect");
        }
    }

    private void connectAndListen() {
        HttpClient httpClient = HttpClient.newHttpClient();

        try {
            webSocket = httpClient.newWebSocketBuilder()
                .buildAsync(URI.create(serviceUrl), new FrameListener())
                .get();
        }
        catch ( Exception e ) {
            System.out.println("Tangible interface server not found at " + serviceUrl + ": " + e.getMessage());
        }
    }

    private void notifyListeners(TangibleInterfaceEvent event) {
        List<TangibleInterfaceListener> snapshot;

        synchronized ( listeners ) {
            snapshot = new ArrayList<TangibleInterfaceListener>(listeners);
        }

        for ( TangibleInterfaceListener listener : snapshot ) {
            listener.tangibleInterfaceEventReceived(event);
        }
    }

    private void processMessage(String message) {
        Matcher groupMatcher = GROUP_PATTERN.matcher(message);

        while ( groupMatcher.find() ) {
            TangibleInterfaceEvent event = parseEvent(groupMatcher.group());
            if ( event != null ) {
                notifyListeners(event);
            }
        }
    }

    private TangibleInterfaceEvent parseEvent(String groupJson) {
        String id = extractString(LABEL_PATTERN, groupJson);
        double[] positionValues = extractNumbers(POSITION_PATTERN, groupJson, 3);
        double[] quaternionValues = extractNumbers(QUATERNION_PATTERN, groupJson, 4);

        if ( id == null || positionValues == null || quaternionValues == null ) {
            return null;
        }

        Vector3Dd position = new Vector3Dd(positionValues[0], positionValues[1], positionValues[2]);
        Vector3Dd direction = new Vector3Dd(quaternionValues[1], quaternionValues[2], quaternionValues[3]);
        Quaterniond rotation = new Quaterniond(direction, quaternionValues[0]);

        return new TangibleInterfaceEvent(id, position, rotation);
    }

    private String extractString(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);

        if ( !matcher.find() ) {
            return null;
        }

        return matcher.group(1);
    }

    private double[] extractNumbers(Pattern pattern, String text, int count) {
        Matcher matcher = pattern.matcher(text);

        if ( !matcher.find() ) {
            return null;
        }

        String[] tokens = matcher.group(1).split(",");
        if ( tokens.length != count ) {
            return null;
        }

        double[] values = new double[count];
        for ( int i = 0; i < count; i++ ) {
            values[i] = Double.parseDouble(tokens[i].trim());
        }

        return values;
    }

    private class FrameListener implements WebSocket.Listener {
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            incomingMessage.append(data);

            if ( last ) {
                String message = incomingMessage.toString();
                incomingMessage.setLength(0);
                processMessage(message);
            }

            webSocket.request(1);
            return null;
        }

        public void onError(WebSocket webSocket, Throwable error) {
            System.out.println("Tangible interface connection error on " + serviceUrl + ": " + error.getMessage());
        }
    }
}
