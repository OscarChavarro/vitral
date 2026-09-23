#include <cstdio>
#include <cstdlib>

#include "java/net/ServerSocket.h"
#include "java/net/Socket.h"
#include "webservice/WebServiceClient.hpp"
#include "webservice/WebServiceServer.hpp"
#include <pthread.h>
WebServiceServer::WebServiceServer(const WebServiceConfig& cfg, MarkerEventBus* bus)
    : config(cfg), bus(bus), shouldStop_(false) {}

WebServiceServer::~WebServiceServer() {
    requestStop();
    pthread_mutex_destroy(&serverSocketMutex);
    pthread_mutex_destroy(&clientThreadsMutex);
}

bool WebServiceServer::start() {
    pthread_mutex_lock(&serverSocketMutex);
    serverSocket_ = new java::net::ServerSocket(config.port);
    pthread_mutex_unlock(&serverSocketMutex);

    if (!serverSocket_->isOpen()) {
        std::fprintf(stderr, "[webservice] failed to bind port %d\n", config.port);
        pthread_mutex_lock(&serverSocketMutex);
        delete serverSocket_;
        serverSocket_ = nullptr;
        pthread_mutex_unlock(&serverSocketMutex);
        return false;
    }
    std::printf("[webservice] listening on port %d, path %s\n",
                config.port, config.path);
    std::printf("[webservice] Test WebSocket messages with: websocat ws://localhost:%d%s\n",
                config.port, config.path);

    while (!shouldStop_) {
        java::net::Socket* clientSocket = serverSocket_->accept();
        if (!clientSocket) {
            if (shouldStop_) break;
            continue;
        }

        if (shouldStop_) {
            delete clientSocket;
            break;
        }

        ClientThreadArg* threadArg = new ClientThreadArg();
        threadArg->server = this;
        threadArg->socketFd = clientSocket->releaseFd();
        delete clientSocket;

        pthread_t thread;
        if (pthread_create(&thread, NULL, &WebServiceServer::clientThreadEntry, threadArg) == 0) {
            pthread_mutex_lock(&clientThreadsMutex);
            clientThreads.push_back(thread);
            pthread_mutex_unlock(&clientThreadsMutex);
        } else {
            delete threadArg;
        }
    }

    pthread_mutex_lock(&clientThreadsMutex);
    for (size_t i = 0; i < clientThreads.size(); ++i) {
        pthread_join(clientThreads[i], NULL);
    }
    clientThreads.clear();
    pthread_mutex_unlock(&clientThreadsMutex);

    pthread_mutex_lock(&serverSocketMutex);
    if (serverSocket_ != nullptr) {
        serverSocket_->close();
        delete serverSocket_;
        serverSocket_ = nullptr;
    }
    pthread_mutex_unlock(&serverSocketMutex);
    return true;
}

void WebServiceServer::requestStop() {
    shouldStop_.store(true);
    pthread_mutex_lock(&serverSocketMutex);
    if (serverSocket_ != nullptr) {
        serverSocket_->close();
    }
    pthread_mutex_unlock(&serverSocketMutex);
}

void* WebServiceServer::clientThreadEntry(void* arg) {
    ClientThreadArg* threadArg = static_cast<ClientThreadArg*>(arg);
    try {
        threadArg->server->handleClient(threadArg->socketFd);
    } catch (...) {}
    delete threadArg;
    return NULL;
}

void WebServiceServer::handleClient(int fd) {
    WebServiceClient client(fd, config, bus);
    client.serve();
}
