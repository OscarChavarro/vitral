#ifndef __WEB_SERVICE_SERVER__
#define __WEB_SERVICE_SERVER__

#include <atomic>
#include <pthread.h>
#include <vector>

#include "java/net/ServerSocket.h"

class MarkerEventBus;
class WebServiceClient;

struct WebServiceConfig {
    int port = 8090;
    const char* path = "/v1/values";
    int streamHz = 30;
};

class WebServiceServer {
public:
    explicit WebServiceServer(const WebServiceConfig& cfg, MarkerEventBus* bus);
    ~WebServiceServer();

    bool start();
    void requestStop();

private:
    struct ClientThreadArg {
        WebServiceServer* server;
        int socketFd;
    };

    static void* clientThreadEntry(void* arg);
    void handleClient(int fd);

    WebServiceConfig config_;
    MarkerEventBus* bus_;
    std::atomic<bool> shouldStop_{false};
    java::net::ServerSocket* serverSocket_{nullptr};
    pthread_mutex_t serverSocketMutex_ = PTHREAD_MUTEX_INITIALIZER;
    std::vector<pthread_t> clientThreads_;
    pthread_mutex_t clientThreadsMutex_ = PTHREAD_MUTEX_INITIALIZER;
};

#endif
