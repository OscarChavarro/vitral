#ifndef __NET__
#define __NET__
#include "net/NetDevice.h"
#include "net/NetInterfaceInfo.h"
#include "net/NetConfig.h"

class Net {
public:
    static constexpr unsigned ANNOUNCEMENTS = 3;
    static constexpr unsigned RX_QUEUE_LIMIT = 256;

    static void init();

    static bool enqueueReceived(PacketBuffer *frame);

    static int64_t describe(unsigned index, NetInterfaceInfo &out);
    static int64_t configure(const NetConfig &config);

    static void timer();

    static unsigned processPending(unsigned limit = RX_QUEUE_LIMIT);
    static unsigned pending();

private:
    static bool selfTest();
};

#endif
