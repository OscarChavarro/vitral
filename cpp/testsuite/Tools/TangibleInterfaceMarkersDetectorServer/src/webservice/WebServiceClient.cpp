#include <cstdio>

#include "java/util/ArrayList.txx"
#include "model/MarkerEventBus.hpp"
#include "webservice/WebServiceClient.hpp"
#include <unistd.h>
WebServiceClient::WebServiceClient(int socketFd, const WebServiceConfig& cfg, MarkerEventBus* bus)
    : socket_(socketFd), config_(cfg), bus_(bus) {}

void WebServiceClient::serve() {
    java::InputStream*  input  = socket_.getInputStream();
    java::OutputStream* output = socket_.getOutputStream();

    if (!protocol_.performHandshake(input, output, config_.path)) return;

    std::printf("[webservice] WebSocket client connected\n");

    for (;;) {
        java::ArrayList<MarkerGroupPose> groups;
        if (!bus_->pollNetwork(&groups)) {
            usleep(10000);
            continue;
        }
        if (groups.size() == 1 && groups.get(0).label == "exit") {
            break;
        }
        if (!protocol_.sendJsonMessage(output, groups)) break;
    }

    protocol_.sendCloseFrame(output);
    std::printf("[webservice] client disconnected\n");
}
