#ifndef __TANGIBLE_INTERFACE_NETWORK_CLIENT__
#define __TANGIBLE_INTERFACE_NETWORK_CLIENT__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/PosixThreads.h"

#include "java/lang/String.h"
#include "java/util/ArrayList.h"
#include "java/util/function/Consumer.h"
#include "vsdk/toolkit/gui/tangibleInterfaces/TangibleInterfaceEvent.h"
class TangibleInterfaceListener;
class WebSocket;

class TangibleInterfaceNetworkClient {
private:
    String serviceUrl;
    ArrayList<TangibleInterfaceListener*> listeners;
    pthread_mutex_t listenersMutex;
    pthread_t connectionThread;
    BOOLEAN threadStarted;
    BOOLEAN stopRequested;
    WebSocket* webSocket;
    Consumer<BOOLEAN>* connectionListener;

    static void* threadEntry(void* arg);
    void connectAndListen();
    void notifyListeners(const TangibleInterfaceEvent& event);
    void processMessage(const String& message);
    BOOLEAN parseEvent(const String& groupJson, TangibleInterfaceEvent* outEvent);
    static BOOLEAN extractString(const String& text, const char* key, String* out);
    static BOOLEAN extractNumbers(const String& text, const char* key, double* values, int count);

public:
    explicit TangibleInterfaceNetworkClient(const String& serviceUrl);
    ~TangibleInterfaceNetworkClient();

    void addListener(TangibleInterfaceListener* listener);
    void removeListener(TangibleInterfaceListener* listener);
    void run();

    /**
    Starts the client and reports whether the websocket handshake succeeds,
    as `run(Consumer<Boolean>)` does in the Java port. The listener, if
    given, runs in the connection thread.
    @param connectionListener receives true once the client is ready to
    receive websocket messages, or false on failure (not owned, may be null)
    */
    void run(Consumer<BOOLEAN>* connectionListener);
    void disconnect();
};

#endif
