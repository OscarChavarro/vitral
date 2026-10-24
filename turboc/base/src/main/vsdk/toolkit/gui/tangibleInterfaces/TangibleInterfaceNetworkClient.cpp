#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "vsdk/toolkit/common/VSDK.h"
#include "java/net/WebSocket.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/gui/tangibleInterfaces/TangibleInterfaceListener.h"
#include "vsdk/toolkit/gui/tangibleInterfaces/TangibleInterfaceNetworkClient.h"

static int findText(const String& text, const char* needle, int fromIndex = 0)
{
    const char* base = text.c_str();
    const char* p = strstr(base + fromIndex, needle);
    if ( p == 0 ) {
        return String::npos;
    }
    return ((int)(p - base));
}

TangibleInterfaceNetworkClient::TangibleInterfaceNetworkClient(const String& serviceUrl)
    : serviceUrl(serviceUrl),
      threadStarted(FALSE),
      stopRequested(FALSE),
      webSocket(0),
      connectionListener(NULL)
{
    pthread_mutex_init(&listenersMutex, 0);
}

TangibleInterfaceNetworkClient::~TangibleInterfaceNetworkClient()
{
    disconnect();
    pthread_mutex_destroy(&listenersMutex);
}

void TangibleInterfaceNetworkClient::addListener(TangibleInterfaceListener* listener)
{
    if ( listener == 0 ) {
        return;
    }

    pthread_mutex_lock(&listenersMutex);
    listeners.add(listener);
    pthread_mutex_unlock(&listenersMutex);
}

void TangibleInterfaceNetworkClient::removeListener(TangibleInterfaceListener* listener)
{
    pthread_mutex_lock(&listenersMutex);
#ifdef __TURBOC__
    listeners.remove2(listener);
#else
    listeners.remove(listener);
#endif
    pthread_mutex_unlock(&listenersMutex);
}

void TangibleInterfaceNetworkClient::run()
{
    run(NULL);
}

void TangibleInterfaceNetworkClient::run(
    Consumer<BOOLEAN>* connectionListener)
{
    if ( threadStarted ) {
        return;
    }
    this->connectionListener = connectionListener;
    threadStarted = pthread_create(&connectionThread, 0, threadEntry, this) == 0;
}

void TangibleInterfaceNetworkClient::disconnect()
{
    stopRequested = TRUE;
    if ( webSocket != 0 ) {
        webSocket->close();
    }
    if ( threadStarted && !pthread_equal(pthread_self(), connectionThread) ) {
        pthread_join(connectionThread, 0);
        threadStarted = FALSE;
    }
    delete webSocket;
    webSocket = 0;
}

void* TangibleInterfaceNetworkClient::threadEntry(void* arg)
{
    TangibleInterfaceNetworkClient* client = ((TangibleInterfaceNetworkClient*)(arg));
    if ( client != 0 ) {
        client->connectAndListen();
    }
    return 0;
}

void TangibleInterfaceNetworkClient::connectAndListen()
{
    webSocket = WebSocket::connect(serviceUrl);
    if ( webSocket == 0 ) {
        printf("Tangible interface server not found at %s\n", serviceUrl.c_str());
        if ( connectionListener != NULL ) {
            connectionListener->accept(FALSE);
        }
        return;
    }
    if ( stopRequested ) {
        webSocket->close();
        return;
    }
    if ( connectionListener != NULL ) {
        connectionListener->accept(TRUE);
    }

    while ( !stopRequested ) {
        String payload;
        if ( !webSocket->readText(&payload) ) {
            break;
        }
        processMessage(payload);
    }
    if ( webSocket != 0 ) {
        webSocket->close();
    }
}

void TangibleInterfaceNetworkClient::notifyListeners(const TangibleInterfaceEvent& event)
{
    ArrayList<TangibleInterfaceListener*> snapshot;

    pthread_mutex_lock(&listenersMutex);
    long i;
    for ( i = 0; i < listeners.size(); i++ ) {
        snapshot.add(listeners.get(i));
    }
    pthread_mutex_unlock(&listenersMutex);

    for ( i = 0; i < snapshot.size(); i++ ) {
        TangibleInterfaceListener* listener = snapshot.get(i);
        if ( listener != 0 ) {
            listener->tangibleInterfaceEventReceived(event);
        }
    }
}

void TangibleInterfaceNetworkClient::processMessage(const String& message)
{
    int len = message.length();
    for ( int i = 0; i < len; i++ ) {
        if ( message[i] != '{' ) {
            continue;
        }
        int start = i;
        while ( i < len && message[i] != '}' ) {
            i++;
        }
        if ( i >= len ) {
            break;
        }
        String groupJson = message.substr(start, i - start + 1);
        TangibleInterfaceEvent event;
        if ( parseEvent(groupJson, &event) ) {
            notifyListeners(event);
        }
    }
}

BOOLEAN TangibleInterfaceNetworkClient::parseEvent(const String& groupJson, TangibleInterfaceEvent* outEvent)
{
    String id;
    double positionValues[3];
    double quaternionValues[4];

    if ( !extractString(groupJson, "label", &id) ||
         !extractNumbers(groupJson, "position", positionValues, 3) ||
         !extractNumbers(groupJson, "quaternion", quaternionValues, 4) ) {
        return FALSE;
    }

    Vector3Dd position(positionValues[0], positionValues[1], positionValues[2]);
    Vector3Dd direction(quaternionValues[1], quaternionValues[2], quaternionValues[3]);
    Quaterniond rotation(direction, quaternionValues[0]);
    *outEvent = TangibleInterfaceEvent(id, position, rotation);
    return TRUE;
}

BOOLEAN TangibleInterfaceNetworkClient::extractString(const String& text, const char* key, String* out)
{
    String needle = String("\"") + key + "\"";
    int p = findText(text, needle.c_str());
    if ( p == String::npos ) {
        return FALSE;
    }
    p = text.find(':', p + needle.length());
    if ( p == String::npos ) {
        return FALSE;
    }
    p = text.find('"', p);
    if ( p == String::npos ) {
        return FALSE;
    }
    int e = text.find('"', p + 1);
    if ( e == String::npos ) {
        return FALSE;
    }
    *out = text.substr(p + 1, e - p - 1);
    return TRUE;
}

BOOLEAN TangibleInterfaceNetworkClient::extractNumbers(const String& text, const char* key, double* values, int count)
{
    String needle = String("\"") + key + "\"";
    int p = findText(text, needle.c_str());
    if ( p == String::npos ) {
        return FALSE;
    }
    p = text.find('[', p + needle.length());
    int e = text.find(']', p);
    if ( p == String::npos || e == String::npos ) {
        return FALSE;
    }

    String payload = text.substr(p + 1, e - p - 1);
    int start = 0;
    for ( int i = 0; i < count; i++ ) {
        int comma = payload.find(',', start);
        String token;
        if ( i == count - 1 ) {
            token = payload.substr(start);
        }
        else {
            token = payload.substr(start, comma - start);
        }
        if ( i != count - 1 && comma == String::npos ) {
            return FALSE;
        }
        values[i] = atof(token.c_str());
        start = comma + 1;
    }
    return TRUE;
}
