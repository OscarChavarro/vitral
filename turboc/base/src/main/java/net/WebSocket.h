#ifndef __WEB_SOCKET__
#define __WEB_SOCKET__

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
    bool open_;

    explicit WebSocket(Socket* socket);

    static bool parseUrl(const String& url, ParsedUrl* outUrl);
    static bool extractStringPrefix(const String& text, const char* prefix);
    static bool readHttpHeader(Socket* socket, String* outHeader);
    static bool sendHandshake(Socket* socket, const ParsedUrl& url);
    static bool readExact(Socket* socket, unsigned char* buffer, int length);
    static bool discard(Socket* socket, unsigned long long length);

public:
    ~WebSocket();

    static WebSocket* connect(const String& serviceUrl);
    bool readText(String* outPayload);
    void close();
    bool isOpen() const;
};


#endif
