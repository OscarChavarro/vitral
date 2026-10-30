#include "drivers/hid/MouseDevice.h"
#include "kernel/Panic.h"
#include "kernel/Scheduler.h"
#include "lib/vitralKernel/LockGuard.h"
#include "drivers/char/CharDevices.h"

MouseDevice::Stream MouseDevice::streams[VirtualConsole::COUNT];
SpinLock MouseDevice::lock;

static const uint8_t INTELLIMOUSE_SEQUENCE[] = {0xF3, 200, 0xF3, 100, 0xF3, 80};
static const uint8_t EXPLORER_SEQUENCE[] = {0xF3, 200, 0xF3, 200, 0xF3, 80};
static constexpr unsigned SEQUENCE_LENGTH = sizeof(INTELLIMOUSE_SEQUENCE);
static constexpr uint8_t ACK = 0xFA;

static int
clamp(int value, int low, int high) { return value < low ? low : value > high ? high
                                                                              : value; }

void
MouseDevice::init() {
    for (Stream &s : streams) {
        s.head = s.tail = 0;
        s.lastButtons = 0;
        s.replyCount = s.packetFirst = s.packetLength = 0;
        s.protocol = Protocol::Ps2;
        s.intelliMatched = s.explorerMatched = 0;
    }
    if (!CharDevices::add(new MouseDevice())) {
        Panic::halt("cannot register /dev/input/mice");
    }
}

void
MouseDevice::report(int dx, int dy, int wheel, uint8_t buttons) {
    LockGuard guard(lock);
    Stream &s = streams[VirtualConsole::active() % VirtualConsole::COUNT];
    if (dx == 0 && dy == 0 && wheel == 0 && buttons == s.lastButtons) {
        return;
    }
    s.lastButtons = buttons;
    unsigned next = (s.head + 1) & (Stream::QUEUE - 1);
    if (s.head != s.tail) {
        Motion &last = s.queue[(s.head - 1) & (Stream::QUEUE - 1)];
        if (last.buttons == buttons || next == s.tail) {
            last.dx += dx;
            last.dy += dy;
            last.wheel += wheel;
            last.buttons = buttons;
            return;
        }
    }
    s.queue[s.head] = Motion{dx, dy, wheel, buttons};
    s.head = next;
}

void
MouseDevice::encode(Stream &s) {
    Motion &m = s.queue[s.tail];
    int dx = clamp(m.dx, -127, 127);
    int up = clamp(-m.dy, -127, 127);
    int wheel = s.protocol == Protocol::Explorer ? clamp(m.wheel, -8, 7) : clamp(m.wheel, -127, 127);
    s.packet[0] = (uint8_t)(0x08 | (m.buttons & 7) | (dx < 0 ? 0x10 : 0) | (up < 0 ? 0x20 : 0));
    s.packet[1] = (uint8_t)dx;
    s.packet[2] = (uint8_t)up;
    s.packetLength = 3;
    if (s.protocol == Protocol::IntelliMouse) {
        s.packet[3] = (uint8_t)wheel;
        s.packetLength = 4;
    } else if (s.protocol == Protocol::Explorer) {
        s.packet[3] = (uint8_t)((wheel & 0x0F) | (m.buttons & FOURTH ? 0x10 : 0) | (m.buttons & FIFTH ? 0x20 : 0));
        s.packetLength = 4;
    } else {
        wheel = 0;
    }
    s.packetFirst = 0;
    m.dx -= dx;
    m.dy += up;
    m.wheel = s.protocol == Protocol::Ps2 ? 0 : m.wheel - wheel;
    if (m.dx == 0 && m.dy == 0 && m.wheel == 0) {
        s.tail = (s.tail + 1) & (Stream::QUEUE - 1);
    }
}

int64_t
MouseDevice::read(void *buffer, size_t count) {
    LockGuard guard(lock);
    Stream &s = streams[Scheduler::currentConsole() % VirtualConsole::COUNT];
    uint8_t *out = (uint8_t *)buffer;
    size_t done = 0;
    while (done < count) {
        if (s.replyCount) {
            out[done++] = s.reply[0];
            for (unsigned i = 1; i < s.replyCount; i++) {
                s.reply[i - 1] = s.reply[i];
            }
            s.replyCount--;
        } else if (s.packetFirst < s.packetLength) {
            out[done++] = s.packet[s.packetFirst++];
        } else if (s.head != s.tail) {
            encode(s);
        } else {
            break;
        }
    }
    return done ? (int64_t)done : -Err::AGAIN;
}

void
MouseDevice::queueReply(Stream &s, uint8_t byte) {
    if (s.replyCount < Stream::REPLY) {
        s.reply[s.replyCount++] = byte;
    }
}

void
MouseDevice::answer(Stream &s, uint8_t command) {
    queueReply(s, ACK);
    if (command == 0xF2) {
        queueReply(s, s.protocol == Protocol::IntelliMouse ? 3 : s.protocol == Protocol::Explorer ? 4 : 0);
    } else if (command == 0xFF) {
        s.protocol = Protocol::Ps2;
        queueReply(s, 0xAA);
        queueReply(s, 0x00);
    }
}

int64_t
MouseDevice::write(const void *buffer, size_t count) {
    LockGuard guard(lock);
    Stream &s = streams[Scheduler::currentConsole() % VirtualConsole::COUNT];
    const uint8_t *in = (const uint8_t *)buffer;
    for (size_t i = 0; i < count; i++) {
        uint8_t c = in[i];
        s.intelliMatched = c == INTELLIMOUSE_SEQUENCE[s.intelliMatched] ? s.intelliMatched + 1 : 0;
        s.explorerMatched = c == EXPLORER_SEQUENCE[s.explorerMatched] ? s.explorerMatched + 1 : 0;
        if (s.intelliMatched == SEQUENCE_LENGTH) {
            s.protocol = Protocol::IntelliMouse;
            s.intelliMatched = 0;
        }
        if (s.explorerMatched == SEQUENCE_LENGTH) {
            s.protocol = Protocol::Explorer;
            s.explorerMatched = 0;
        }
        answer(s, c);
    }
    s.packetFirst = s.packetLength = 0;
    return (int64_t)count;
}

bool
MouseDevice::readable() {
    LockGuard guard(lock);
    const Stream &s = streams[Scheduler::currentConsole() % VirtualConsole::COUNT];
    return s.replyCount || s.packetFirst < s.packetLength || s.head != s.tail;
}
