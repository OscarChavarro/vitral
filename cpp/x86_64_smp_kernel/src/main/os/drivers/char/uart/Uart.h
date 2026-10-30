#ifndef __UART__
#define __UART__
#include <stddef.h>

class Uart {
  public:
    static void initCom1();
    static bool ready() { return enabled; }
    static void write(const char *text, size_t length);

  private:
    static constexpr unsigned COM1 = 0x3F8;
    static constexpr unsigned DATA = 0;
    static constexpr unsigned INTERRUPT_ENABLE = 1;
    static constexpr unsigned FIFO_CONTROL = 2;
    static constexpr unsigned LINE_CONTROL = 3;
    static constexpr unsigned MODEM_CONTROL = 4;
    static constexpr unsigned LINE_STATUS = 5;
    static constexpr unsigned DLAB = 0x80;
    static constexpr unsigned TRANSMITTER_EMPTY = 0x20;
    static constexpr unsigned DIVISOR_9600 = 12;
    static constexpr unsigned WAIT_LIMIT = 100000;

    static bool enabled;
    static bool waitForTransmitter();
};

#endif
