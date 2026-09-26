#include <cerrno>
#include <cstdio>

#include "java/net/ServerSocket.h"
#include "java/net/Socket.h"
#include "application/mcp/XtOpenGL1VitralEditorMCP.h"
#include "application/mcp/XtOpenGL1VitralEditorMCPProtocol.h"

namespace {

/**
One connection and the thread serving it: destroyed by that thread when the
connection ends, as the Java garbage collector would do.
*/
class ClientConnection : public java::Runnable {
private:
    java::net::Socket* socket;
    XtOpenGL1VitralEditorMCPProtocol* listener;
    java::Thread* thread;

public:
    ClientConnection(XtOpenGL1SceneEditorApplication* parent,
                     java::net::Socket* socket)
        : socket(socket),
          listener(new XtOpenGL1VitralEditorMCPProtocol(parent, socket)),
          thread(new java::Thread(this))
    {
        thread->setName("VitralEditorMCPClient");
    }

    void start()
    {
        thread->start();
    }

    virtual void run() override
    {
        listener->run();
        delete listener;
        delete socket;
        delete thread;
        delete this;
    }
};

}

XtOpenGL1VitralEditorMCP::XtOpenGL1VitralEditorMCP(
    XtOpenGL1SceneEditorApplication* parent)
    : parent(parent), tcpPort(1234), networkThread(this)
{
    networkThread.setName("XtOpenGL1VitralEditorMCP");
    networkThread.start();
}

void XtOpenGL1VitralEditorMCP::run()
{
    printf("Waiting for MCP connections on TCP port %d\n", tcpPort);
    fflush(stdout);

    java::net::ServerSocket serverSocket(tcpPort);
    if ( !serverSocket.isOpen() ) {
        fprintf(stderr, "Error in XtOpenGL1VitralEditorMCP communications!\n");
        fprintf(stderr, "Can not listen on TCP port %d\n", tcpPort);
        return;
    }
    while ( true ) {
        java::net::Socket* clientSocket = serverSocket.accept();
        if ( clientSocket == nullptr ) {
            if ( errno == EINTR || errno == ECONNABORTED ) {
                continue;
            }
            fprintf(stderr, "Error in XtOpenGL1VitralEditorMCP communications!\n");
            return;
        }
        ClientConnection* connection =
            new ClientConnection(parent, clientSocket);
        connection->start();
    }
}
