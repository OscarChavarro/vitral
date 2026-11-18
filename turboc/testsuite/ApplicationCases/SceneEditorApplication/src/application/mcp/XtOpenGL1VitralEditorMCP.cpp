#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#include "vsdk/toolkit/common/VSDK.h"
#ifdef VITRAL_WITHOUT_PTHREADS
#include "vsdk/toolkit/gui/XtIntrinsics.h"
#include "vsdk/toolkit/gui/XtEventQueue.h"
#endif
#include "java/net/ServerSocket.h"
#include "java/net/Socket.h"
#include "application/mcp/XtOpenGL1VitralEditorMCP.h"
#include "application/mcp/XtOpenGL1VitralEditorMCPProtocol.h"


/**
One connection and the thread serving it: destroyed by that thread when the
connection ends, as the Java garbage collector would do.
*/
class ClientConnection : public Runnable {
private:
    Socket* socket;
    XtOpenGL1VitralEditorMCPProtocol* listener;
    Thread* thread;

public:
    ClientConnection(XtOpenGL1SceneEditorApplication* parent,
                     Socket* socket)
        : socket(socket),
          listener(new XtOpenGL1VitralEditorMCPProtocol(parent, socket)),
          thread(new Thread(this))
    {
        thread->setName("VitralEditorMCPClient");
    }

    void start()
    {
        thread->start();
    }

    virtual void run()
    {
        listener->run();
        delete listener;
        delete socket;
        delete thread;
        delete this;
    }
};


#ifdef VITRAL_WITHOUT_PTHREADS

/*
C++ port note: without threads (`VITRAL_WITHOUT_PTHREADS`, i.e. Debian 1.1,
see legacy.mk) `Thread::start` runs nothing, so the service would never
listen. Instead the event loop of Xt watches the listening socket and each
connection (`XtAppAddInput`), and the requests are answered in the thread of
the GUI, where `XtEventQueue::invokeAndWait` runs the tools at once. The
protocol, its messages and its answers are the same.
*/

/**
One connection served from the event loop: buffers what arrives and answers
each complete line. Destroyed when the client closes it.
*/
class EventLoopConnection {
private:
    Socket* socket;
    XtOpenGL1VitralEditorMCPProtocol* protocol;
    XtInputId input;
    char* pending;
    int pendingBytes;
    int pendingCapacity;

    void append(const char* bytes, int count)
    {
        if ( pendingBytes + count + 1 > pendingCapacity ) {
            int capacity = pendingCapacity * 2;
            while ( capacity < pendingBytes + count + 1 ) {
                capacity *= 2;
            }
            pending = (char*)realloc(pending, capacity);
            pendingCapacity = capacity;
        }
        memcpy(pending + pendingBytes, bytes, count);
        pendingBytes += count;
    }

    BOOLEAN writeAll(const char* bytes, int count)
    {
        while ( count > 0 ) {
            int written = (int)write(socket->fd(), bytes, count);
            if ( written < 0 && errno == EINTR ) {
                continue;
            }
            if ( written <= 0 ) {
                return FALSE;
            }
            bytes += written;
            count -= written;
        }
        return TRUE;
    }

    /** Answers the complete lines of the buffer: false when the client is gone */
    BOOLEAN answerLines()
    {
        int start = 0;
        int i;
        for ( i = 0; i < pendingBytes; i++ ) {
            if ( pending[i] != '\n' ) {
                continue;
            }
            int end = i;
            if ( end > start && pending[end - 1] == '\r' ) {
                end--;
            }
            pending[end] = '\0';
            String answer = protocol->handle(String(pending + start));
            if ( !writeAll(answer.c_str(), answer.length()) ||
                 !writeAll("\n", 1) ) {
                return FALSE;
            }
            start = i + 1;
        }
        memmove(pending, pending + start, pendingBytes - start);
        pendingBytes -= start;
        return TRUE;
    }

    static void readable(XtPointer data, int* /*fd*/, XtInputId* /*id*/)
    {
        EventLoopConnection* connection = (EventLoopConnection*)data;
        char bytes[4096];
        int count = (int)read(connection->socket->fd(), bytes, sizeof(bytes));
        if ( count < 0 && errno == EINTR ) {
            return;
        }
        if ( count <= 0 ) {
            delete connection;
            return;
        }
        connection->append(bytes, count);
        if ( !connection->answerLines() ) {
            delete connection;
        }
    }

public:
    EventLoopConnection(XtOpenGL1SceneEditorApplication* parent,
                        Socket* socket)
        : socket(socket),
          protocol(new XtOpenGL1VitralEditorMCPProtocol(parent, socket)),
          pending((char*)malloc(4096)), pendingBytes(0),
          pendingCapacity(4096)
    {
        input = XtAppAddInput(XtEventQueue::getApplicationContext(),
            socket->fd(), ((XtPointer)(XtInputReadMask)),
            &EventLoopConnection::readable, (XtPointer)this);
    }

    ~EventLoopConnection()
    {
        XtRemoveInput(input);
        delete protocol;
        delete socket;
        free(pending);
    }
};

/**
The listening socket, watched by the event loop for as long as the process
lives.
*/
class EventLoopListener {
private:
    XtOpenGL1SceneEditorApplication* parent;
    ServerSocket serverSocket;

    static void acceptable(XtPointer data, int* /*fd*/, XtInputId* /*id*/)
    {
        EventLoopListener* listener = (EventLoopListener*)data;
        Socket* clientSocket = listener->serverSocket.accept();
        if ( clientSocket != NULL ) {
            new EventLoopConnection(listener->parent, clientSocket);
        }
    }

public:
    EventLoopListener(XtOpenGL1SceneEditorApplication* parent, int tcpPort)
        : parent(parent), serverSocket(tcpPort)
    {
    }

    BOOLEAN start(int listeningFd)
    {
        XtAppAddInput(XtEventQueue::getApplicationContext(), listeningFd,
            ((XtPointer)(XtInputReadMask)), &EventLoopListener::acceptable,
            (XtPointer)this);
        return TRUE;
    }

    BOOLEAN isOpen() const
    {
        return serverSocket.isOpen();
    }

    int listeningFd() const
    {
        return serverSocket.fd();
    }
};

XtOpenGL1VitralEditorMCP::XtOpenGL1VitralEditorMCP(
    XtOpenGL1SceneEditorApplication* parent)
    : parent(parent), tcpPort(1234), networkThread(this)
{
    printf("Waiting for MCP connections on TCP port %d\n", tcpPort);
    fflush(stdout);
    EventLoopListener* listener = new EventLoopListener(parent, tcpPort);
    if ( !listener->isOpen() ) {
        fprintf(stderr, "Error in XtOpenGL1VitralEditorMCP communications!\n");
        fprintf(stderr, "Can not listen on TCP port %d\n", tcpPort);
        delete listener;
        return;
    }
    listener->start(listener->listeningFd());
}

#else

XtOpenGL1VitralEditorMCP::XtOpenGL1VitralEditorMCP(
    XtOpenGL1SceneEditorApplication* parent)
    : parent(parent), tcpPort(1234), networkThread(this)
{
    networkThread.setName("XtOpenGL1VitralEditorMCP");
    networkThread.start();
}

#endif

void XtOpenGL1VitralEditorMCP::run()
{
    printf("Waiting for MCP connections on TCP port %d\n", tcpPort);
    fflush(stdout);

    ServerSocket serverSocket(tcpPort);
    if ( !serverSocket.isOpen() ) {
        fprintf(stderr, "Error in XtOpenGL1VitralEditorMCP communications!\n");
        fprintf(stderr, "Can not listen on TCP port %d\n", tcpPort);
        return;
    }
    while ( TRUE ) {
        Socket* clientSocket = serverSocket.accept();
        if ( clientSocket == NULL ) {
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
