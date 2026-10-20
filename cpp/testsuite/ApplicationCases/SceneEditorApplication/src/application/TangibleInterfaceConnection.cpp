#include <cstring>

#include "application/TangibleInterfaceConnection.h"
#include "model/ApplicationModel.h"
#include "vsdk/toolkit/gui/gizmo/RayGizmo.h"
#include "vsdk/toolkit/gui/tangibleInterfaces/TangibleInterfaceEvent2RayGizmoMapper.h"
#include "vsdk/toolkit/gui/tangibleInterfaces/TangibleInterfaceNetworkClient.h"

namespace {
const char* DEFAULT_TANGIBLE_SERVICE_URL = "ws://127.0.0.1:8090/v1/values";
const char* RAY_CUBE_TANGIBLE_ELEMENT_ID = "rayCube1";

std::string trim(const std::string& text)
{
    const char* blanks = " \t\r\n";
    size_t start = text.find_first_not_of(blanks);
    if ( start == std::string::npos ) {
        return "";
    }
    size_t end = text.find_last_not_of(blanks);
    return text.substr(start, end - start + 1);
}
}

TangibleInterfaceConnection::TangibleInterfaceConnection(
    const std::function<void()>& eventArrived)
    : serviceUrl(DEFAULT_TANGIBLE_SERVICE_URL),
      client(nullptr),
      eventArrived(eventArrived),
      hasPendingEvent(false)
{
    pthread_mutex_init(&pendingMutex, nullptr);
}

TangibleInterfaceConnection::~TangibleInterfaceConnection()
{
    delete client;
    client = nullptr;
    pthread_mutex_destroy(&pendingMutex);
}

void TangibleInterfaceConnection::processArguments(int argc, char** argv)
{
    for ( int i = 1; i < argc; i++ ) {
        if ( std::strcmp(argv[i], "-tangibleServer") == 0 && i + 1 < argc ) {
            setServiceUrl(argv[++i]);
        }
    }
}

const std::string& TangibleInterfaceConnection::getServiceUrl() const
{
    return serviceUrl;
}

void TangibleInterfaceConnection::setServiceUrl(const std::string& serviceUrl)
{
    std::string url = trim(serviceUrl);
    if ( !url.empty() ) {
        this->serviceUrl = url;
    }
}

void TangibleInterfaceConnection::connect(
    const std::string& serviceUrl,
    const std::function<void(bool)>& connectionListener)
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
    if ( notify && eventArrived ) {
        eventArrived();
    }
}

bool TangibleInterfaceConnection::applyPendingEvent(ApplicationModel* model)
{
    pthread_mutex_lock(&pendingMutex);
    bool pending = hasPendingEvent;
    TangibleInterfaceEvent event = pendingEvent;
    hasPendingEvent = false;
    pthread_mutex_unlock(&pendingMutex);

    if ( !pending || model == nullptr || model->getRayGizmo() == nullptr ) {
        return false;
    }
    model->setWithVisualDebugRay(true);
    model->getRayGizmo()->setRollIndicatorEnabled(true);
    TangibleInterfaceEvent2RayGizmoMapper(model->getActiveCamera())
        .map(event, model->getRayGizmo());
    return true;
}
