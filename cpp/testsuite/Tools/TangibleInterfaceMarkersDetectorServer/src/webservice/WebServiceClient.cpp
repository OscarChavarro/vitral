#include <cstdio>

#include "java/util/ArrayList.txx"
#include "model/MarkerEventBus.hpp"
#include "webservice/WebServiceClient.hpp"
#include <unistd.h>
WebServiceClient::WebServiceClient(int socketFd, const WebServiceConfig& cfg, MarkerEventBus* bus)
    : socket(socketFd), config(cfg), bus(bus) {}

void WebServiceClient::serve() {
    java::InputStream*  input  = socket.getInputStream();
    java::OutputStream* output = socket.getOutputStream();

    if (!protocol.performHandshake(input, output, config.path)) return;

    std::printf("[webservice] WebSocket client connected\n");

    for (;;) {
        java::ArrayList<MarkerGroupPose> groups;
        if (!bus->pollNetwork(&groups)) {
            usleep(10000);
            continue;
        }
        if (groups.size() == 1 && groups.get(0).label == "exit") {
            break;
        }
        if (!protocol.sendJsonMessage(output, groups)) break;
    }

    protocol.sendCloseFrame(output);
    std::printf("[webservice] client disconnected\n");
}
