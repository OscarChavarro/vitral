#include "net/NetDevices.h"
#include "net/NetDevice.h"
#include "fs/Err.h"
#include "net/Ethernet.h"
#include "net/Net.h"
#include "lib/vitralKernel/String.h"

NetDevice *NetDevices::table[NetDevices::MAX];

unsigned NetDevices::total = 0;

bool
NetDevices::add(NetDevice *device) {
    if (total >= MAX || find(device->name())) {
        return false;
    }
    table[total++] = device;
    return true;
}

unsigned
NetDevices::count() { return total; }

NetDevice *
NetDevices::at(unsigned index) { return index < total ? table[index] : nullptr; }

NetDevice *
NetDevices::find(const char *name) {
    for (unsigned i = 0; i < total; i++) {
        if (String::equals(table[i]->name(), name)) {
            return table[i];
        }
    }
    return nullptr;
}
