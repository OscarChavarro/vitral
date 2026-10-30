#ifndef __LOOPBACK_DEVICE__
#define __LOOPBACK_DEVICE__
#include "net/NetDevice.h"

class LoopbackDevice : public NetDevice {
public:
    LoopbackDevice() : NetDevice("lo", MacAddress()) {}

    const char *driver() const override { return "loopback"; }
    bool needsArp() const override { return false; }
    bool isLoopback() const override { return true; }

protected:
    int send(PacketBuffer *frame) override;
};

#endif
