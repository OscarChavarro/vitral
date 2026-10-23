#ifndef __WEB_SOCKET__
#define __WEB_SOCKET__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/String.h"


class Socket;

class WebSocket {
private:
    struct ParsedUrl {
        String host;
        int port;
        String path;
    };

    Socket* socket_;
    BOOLEAN open_;

    explicit WebSocket(Socket* socket);

    static BOOLEAN parseUrl(const String& url, ParsedUrl* outUrl);
    static BOOLEAN extractStringPrefix(const String& text, const char* prefix);
    static BOOLEAN readHttpHeader(Socket* socket, String* outHeader);
    static BOOLEAN sendHandshake(Socket* socket, const ParsedUrl& url);
    static BOOLEAN readExact(Socket* socket, unsigned char* buffer, int length);
    static BOOLEAN discard(Socket* socket, unsigned long long length);

public:
    ~WebSocket();

    static WebSocket* connect(const String& serviceUrl);
    BOOLEAN readText(String* outPayload);
    void close();
    BOOLEAN isOpen() const;
};


#endif
