#ifndef __PS_MOUSE__
#define __PS_MOUSE__
#include <stdint.h>
#include "kernel/TrapFrame.h"

class PsMouse {
public:
    static bool init();
    static bool present() { return found; }
    static const char *model();

private:
    static constexpr uint16_t DATA = 0x60;
    static constexpr uint16_t STATUS = 0x64;
    static constexpr uint16_t COMMAND = 0x64;
    static constexpr uint8_t STATUS_OUTPUT_FULL = 0x01;
    static constexpr uint8_t STATUS_INPUT_FULL = 0x02;
    static constexpr uint8_t STATUS_AUX = 0x20;
    static constexpr uint8_t ACK = 0xFA;

    static bool found;
    static uint8_t id;
    static uint8_t packet[4];
    static unsigned received;

    static bool waitInput();
    static bool waitOutput();
    static bool readAux(uint8_t &byte);
    static bool sendCommand(uint8_t command);
    static bool setSampleRate(uint8_t rate);
    static int readId();
    static void disable();
    static TrapFrame *onIrq(TrapFrame *frame);
};

#endif
