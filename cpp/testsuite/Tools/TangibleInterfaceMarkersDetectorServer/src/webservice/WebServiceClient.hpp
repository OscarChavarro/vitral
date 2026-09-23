#ifndef __WEB_SERVICE_CLIENT__
#define __WEB_SERVICE_CLIENT__

#include <chrono>

#include "java/net/Socket.h"
#include "webservice/WebSocketProtocol.hpp"
#include "webservice/WebServiceServer.hpp"

class MarkerEventBus;

class WebServiceClient {
public:
    WebServiceClient(int socketFd, const WebServiceConfig& cfg, MarkerEventBus* bus);

    void serve();

private:
    java::net::Socket socket;
    WebSocketProtocol protocol;
    WebServiceConfig config;
    MarkerEventBus* bus;
};

#endif
