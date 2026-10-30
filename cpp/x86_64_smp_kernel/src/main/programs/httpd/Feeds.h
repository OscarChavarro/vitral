#ifndef __FEEDS__
#define __FEEDS__
#include "programs/httpd/WebSocket.h"

class Feeds {
public:
    enum Kind : uint8_t { SYSTEM = 0x01, PROCESS_INFO = 0x02, PROCESS_STATE = 0x03, LOG = 0x10, CPU_LOAD = 0x20 };
    static constexpr uint32_t DEFAULT_INTERVAL_MS = 500;
    static constexpr uint32_t LOG_INTERVAL_MS = 250;
    static constexpr uint32_t PING_INTERVAL_MS = 10000;

    static void serve(WebSocket &socket, const char *topic);
    static bool knowsTopic(const char *topic);

private:
    class SystemFeed;
    class LogFeed;
    class CpuFeed;
};

#endif
