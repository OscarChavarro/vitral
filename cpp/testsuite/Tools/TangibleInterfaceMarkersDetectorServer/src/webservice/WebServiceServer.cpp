#include <cstdio>
#include <cstdlib>

#include "java/net/ServerSocket.h"
#include "java/net/Socket.h"
#include "webservice/WebServiceClient.hpp"
#include "webservice/WebServiceServer.hpp"
#include <pthread.h>
WebServiceServer::WebServiceServer(const WebServiceConfig& cfg, MarkerEventBus* bus)
    : config_(cfg), bus_(bus), shouldStop_(false) {}

WebServiceServer::~WebServiceServer() {
    requestStop();
    pthread_mutex_destroy(&serverSocketMutex_);
    pthread_mutex_destroy(&clientThreadsMutex_);
}

bool WebServiceServer::start() {
    pthread_mutex_lock(&serverSocketMutex_);
    serverSocket_ = new java::net::ServerSocket(config_.port);
    pthread_mutex_unlock(&serverSocketMutex_);

    if (!serverSocket_->isOpen()) {
        std::fprintf(stderr, "[webservice] failed to bind port %d\n", config_.port);
        pthread_mutex_lock(&serverSocketMutex_);
        delete serverSocket_;
        serverSocket_ = nullptr;
        pthread_mutex_unlock(&serverSocketMutex_);
        return false;
    }
    std::printf("[webservice] listening on port %d, path %s\n",
                config_.port, config_.path);
    std::printf("[webservice] Test WebSocket messages with: websocat ws://localhost:%d%s\n",
                config_.port, config_.path);

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
            pthread_mutex_lock(&clientThreadsMutex_);
            clientThreads_.push_back(thread);
            pthread_mutex_unlock(&clientThreadsMutex_);
        } else {
            delete threadArg;
        }
    }

    pthread_mutex_lock(&clientThreadsMutex_);
    for (size_t i = 0; i < clientThreads_.size(); ++i) {
        pthread_join(clientThreads_[i], NULL);
    }
    clientThreads_.clear();
    pthread_mutex_unlock(&clientThreadsMutex_);

    pthread_mutex_lock(&serverSocketMutex_);
    if (serverSocket_ != nullptr) {
        serverSocket_->close();
        delete serverSocket_;
        serverSocket_ = nullptr;
    }
    pthread_mutex_unlock(&serverSocketMutex_);
    return true;
}

void WebServiceServer::requestStop() {
    shouldStop_.store(true);
    pthread_mutex_lock(&serverSocketMutex_);
    if (serverSocket_ != nullptr) {
        serverSocket_->close();
    }
    pthread_mutex_unlock(&serverSocketMutex_);
}

void* WebServiceServer::clientThreadEntry(void* arg) {
    ClientThreadArg* threadArg = static_cast<ClientThreadArg*>(arg);
    threadArg->server->handleClient(threadArg->socketFd);
    delete threadArg;
    return NULL;
}

void WebServiceServer::handleClient(int fd) {
    WebServiceClient client(fd, config_, bus_);
    client.serve();
}
