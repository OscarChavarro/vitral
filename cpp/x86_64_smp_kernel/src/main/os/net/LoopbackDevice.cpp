#include "net/LoopbackDevice.h"

int
LoopbackDevice::send(PacketBuffer *frame) {
    receive(frame);
    return 0;
}
