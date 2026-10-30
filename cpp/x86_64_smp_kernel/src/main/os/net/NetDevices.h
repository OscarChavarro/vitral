#ifndef __NET_DEVICES__
#define __NET_DEVICES__
#include "net/NetDevice.h"

class NetDevices {
public:
    static constexpr unsigned MAX = 8;

    static bool add(NetDevice *device);
    static unsigned count();
    static NetDevice *at(unsigned index);
    static NetDevice *find(const char *name);

private:
    static NetDevice *table[MAX];
    static unsigned total;
};

#endif
