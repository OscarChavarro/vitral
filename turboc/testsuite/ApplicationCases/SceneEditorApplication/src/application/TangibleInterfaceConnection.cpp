#include <string.h>

#include "application/TangibleInterfaceConnection.h"
#include "model/ApplicationModel.h"
#include "vsdk/toolkit/gui/gizmo/RayGizmo.h"
#include "vsdk/toolkit/gui/tangibleInterfaces/TangibleInterfaceEvent2RayGizmoMapper.h"
#include "vsdk/toolkit/gui/tangibleInterfaces/TangibleInterfaceNetworkClient.h"

static const char* DEFAULT_TANGIBLE_SERVICE_URL = "ws://127.0.0.1:8090/v1/values";
static const char* RAY_CUBE_TANGIBLE_ELEMENT_ID = "rayCube1";

static String trim(const String& text)
{
    const char* blanks = " \t\r\n";
    int start = 0;
    int end = text.size();
    while ( start < end && strchr(blanks, text[start]) != NULL ) {
        start++;
    }
    while ( end > start && strchr(blanks, text[end - 1]) != NULL ) {
        end--;
    }
    return text.substring(start, end);
}

TangibleInterfaceConnection::TangibleInterfaceConnection(
    Runnable* eventArrived)
    : serviceUrl(DEFAULT_TANGIBLE_SERVICE_URL),
      client(NULL),
      eventArrived(eventArrived),
      hasPendingEvent(false)
{
    pthread_mutex_init(&pendingMutex, NULL);
}

TangibleInterfaceConnection::~TangibleInterfaceConnection()
{
    delete client;
    client = NULL;
    pthread_mutex_destroy(&pendingMutex);
}

void TangibleInterfaceConnection::processArguments(int argc, char** argv)
{
    for ( int i = 1; i < argc; i++ ) {
        if ( strcmp(argv[i], "-tangibleServer") == 0 && i + 1 < argc ) {
            setServiceUrl(argv[++i]);
        }
    }
}

const String& TangibleInterfaceConnection::getServiceUrl() const
{
    return serviceUrl;
}

void TangibleInterfaceConnection::setServiceUrl(const String& serviceUrl)
{
    String url = trim(serviceUrl);
    if ( !url.isEmpty() ) {
        this->serviceUrl = url;
    }
}

void TangibleInterfaceConnection::connect(
    const String& serviceUrl,
    Consumer<bool>* connectionListener)
{
    setServiceUrl(serviceUrl);
    // Stops the previous client (and its thread) before the new one
    delete client;
    client = new TangibleInterfaceNetworkClient(this->serviceUrl.c_str());
    client->addListener(this);
    client->run(connectionListener);
}

void TangibleInterfaceConnection::tangibleInterfaceEventReceived(
    const TangibleInterfaceEvent& event)
{
    if ( !event.getId().equals(RAY_CUBE_TANGIBLE_ELEMENT_ID) ) {
        return;
    }
    pthread_mutex_lock(&pendingMutex);
    bool notify = !hasPendingEvent;
    pendingEvent = event;
    hasPendingEvent = true;
    pthread_mutex_unlock(&pendingMutex);
    if ( notify && eventArrived != NULL ) {
        eventArrived->run();
    }
}

bool TangibleInterfaceConnection::applyPendingEvent(ApplicationModel* model)
{
    pthread_mutex_lock(&pendingMutex);
    bool pending = hasPendingEvent;
    TangibleInterfaceEvent event = pendingEvent;
    hasPendingEvent = false;
    pthread_mutex_unlock(&pendingMutex);

    if ( !pending || model == NULL || model->getRayGizmo() == NULL ) {
        return false;
    }
    model->setWithVisualDebugRay(true);
    model->getRayGizmo()->setRollIndicatorEnabled(true);
    TangibleInterfaceEvent2RayGizmoMapper(model->getActiveCamera())
        .map(event, model->getRayGizmo());
    return true;
}
