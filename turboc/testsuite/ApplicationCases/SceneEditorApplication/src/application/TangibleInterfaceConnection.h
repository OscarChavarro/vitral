#ifndef __TANGIBLE_INTERFACE_CONNECTION__
#define __TANGIBLE_INTERFACE_CONNECTION__

#include "java/lang/Runnable.h"
#include "java/util/function/Consumer.h"
#include "java/util/function/Function.h"
#include "java/util/function/Supplier.h"
#include "java/lang/PosixThreads.h"
#include "java/lang/String.h"

#include "vsdk/toolkit/gui/tangibleInterfaces/TangibleInterfaceEvent.h"
#include "vsdk/toolkit/gui/tangibleInterfaces/TangibleInterfaceListener.h"

class ApplicationModel;
class TangibleInterfaceNetworkClient;

/**
Connection of the editor with a tangible interface websocket service (see
`cpp/testsuite/Tools/TangibleInterfaceMarkersDetectorServer`), as the
tangible interface methods of `AwtJogl4SceneEditorApplication` in the Java
application: the poses of the `rayCube1` marker group move the ray gizmo
of the application model.

It does not depend on any GUI technology. The events arrive in the thread
of the network client: this class keeps the last one and notifies its
arrival, so each GUI applies it (`applyPendingEvent`) in the thread of its
event loop, as the Java application repaints with `invokeLater`.
*/
class TangibleInterfaceConnection : private TangibleInterfaceListener {
private:
    String serviceUrl;
    TangibleInterfaceNetworkClient* client;
    Runnable* eventArrived;
    pthread_mutex_t pendingMutex;
    bool hasPendingEvent;
    TangibleInterfaceEvent pendingEvent;

    void setServiceUrl(const String& serviceUrl);
    virtual void tangibleInterfaceEventReceived(
        const TangibleInterfaceEvent& event);

    TangibleInterfaceConnection(const TangibleInterfaceConnection& other);
    TangibleInterfaceConnection& operator=(
        const TangibleInterfaceConnection& other);

public:
    /**
    @param eventArrived called from the thread of the network client when an
    event is waiting to be applied (once until it is applied): it must ask
    the GUI thread to call `applyPendingEvent` (not owned)
    */
    explicit TangibleInterfaceConnection(Runnable* eventArrived);
    ~TangibleInterfaceConnection();

    /**
    Takes the service from the `-tangibleServer <url>` option, if given.
    */
    void processArguments(int argc, char** argv);

    /**
    @return websocket URL of the service last used (or the default one)
    */
    const String& getServiceUrl() const;

    /**
    Connects to a service, replacing the current connection.
    @param serviceUrl websocket URL of the service
    @param connectionListener receives, in the thread of the network
    client, whether the connection succeeded (not owned: it must live
    until the next `connect`, or the destruction of this object)
    */
    void connect(const String& serviceUrl,
                 Consumer<bool>* connectionListener);

    /**
    Moves the ray gizmo of the model to the pose of the last event received.
    Must be called from the thread of the GUI.
    @return true if there was an event to apply (the view must be repainted)
    */
    bool applyPendingEvent(ApplicationModel* model);
};

#endif
