#ifndef __UART__
#define __UART__
#include <stddef.h>
#include "lib/vitralKernel/SpinLock.h"

// COM1 at 9600 baud, 8N1, written by polling. A port that is missing or drains much more slowly than its baud rate (a
// QEMU serial console that cannot keep up, a stalled line) must not hold back the kernel: when a write takes over three
// times as long as 9600 baud need, or one character waits WAIT_LIMIT polls, COM1 is released and ready() becomes false
// for good.
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

    // The time spent in a write is measured in rounds of PIT channel 2, the PC speaker's counter, which every PC has
    // and which works before the clock is calibrated and on CPUs without a time stamp counter. Mode 0 with the
    // largest count: its output (port 0x61 bit 5) rises after 65535 / 1193182 s, about 55 ms.
    static constexpr unsigned PIT_CHANNEL2 = 0x42;
    static constexpr unsigned PIT_COMMAND = 0x43;
    static constexpr unsigned SYSTEM_CONTROL = 0x61;
    static constexpr unsigned CHANNEL2_GATE = 0x01;
    static constexpr unsigned SPEAKER_DATA = 0x02;
    static constexpr unsigned CHANNEL2_OUTPUT = 0x20;
    static constexpr unsigned CHANNEL2_MODE0 = 0xB0;
    // 9600 baud send about 53 characters in a round; a write may last 2 rounds plus one per 16 characters.
    static constexpr unsigned SPARE_ROUNDS = 2;
    static constexpr unsigned CHARACTERS_PER_ROUND = 16;

    static bool enabled;
    static unsigned rounds;
    static unsigned allowedRounds;
    static SpinLock lock;

    static void startRound();
    static bool waitForTransmitter();
};

#endif
