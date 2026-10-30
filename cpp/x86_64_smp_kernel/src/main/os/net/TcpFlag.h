#ifndef __TCP_FLAG__
#define __TCP_FLAG__
#include <stdint.h>

class TcpFlag {
public:
    static constexpr uint8_t FIN = 1;
    static constexpr uint8_t SYN = 2;
    static constexpr uint8_t RST = 4;
    static constexpr uint8_t PSH = 8;
    static constexpr uint8_t ACK = 16;
};

#endif
