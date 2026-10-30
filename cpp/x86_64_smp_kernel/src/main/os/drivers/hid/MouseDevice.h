#ifndef __MOUSE_DEVICE__
#define __MOUSE_DEVICE__
#include <stdint.h>
#include "drivers/char/CharDevice.h"
#include "drivers/video/virtualConsole/VirtualConsole.h"
#include "lib/vitralKernel/SpinLock.h"

class MouseDevice : public CharDevice {
public:
    static constexpr uint8_t LEFT = 1;
    static constexpr uint8_t RIGHT = 2;
    static constexpr uint8_t MIDDLE = 4;
    static constexpr uint8_t FOURTH = 8;
    static constexpr uint8_t FIFTH = 16;

    static void init();
    static void report(int dx, int dy, int wheel, uint8_t buttons);

    int64_t read(void *buffer, size_t count) override;
    int64_t write(const void *buffer, size_t count) override;
    bool readable() override;

private:
    enum class Protocol : uint8_t { Ps2, IntelliMouse, Explorer };

    class Motion {
    public:
        int32_t dx;
        int32_t dy;
        int32_t wheel;
        uint8_t buttons;
    };

    class Stream {
    public:
        static constexpr unsigned QUEUE = 32;
        static constexpr unsigned REPLY = 8;
        Motion queue[QUEUE];
        unsigned head;
        unsigned tail;
        uint8_t lastButtons;
        uint8_t reply[REPLY];
        unsigned replyCount;
        uint8_t packet[4];
        unsigned packetFirst;
        unsigned packetLength;
        Protocol protocol;
        unsigned intelliMatched;
        unsigned explorerMatched;
    };

    MouseDevice() : CharDevice("input", "mice") {}

    static Stream streams[VirtualConsole::COUNT];
    static SpinLock lock;

    static void encode(Stream &s);
    static void answer(Stream &s, uint8_t command);
    static void queueReply(Stream &s, uint8_t byte);
};

#endif
