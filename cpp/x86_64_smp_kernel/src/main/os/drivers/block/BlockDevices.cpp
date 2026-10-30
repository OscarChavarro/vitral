#include "drivers/block/BlockDevices.h"
#include "drivers/block/BlockDevice.h"
#include "drivers/block/LoopDevice.h"
#include "fs/Err.h"
#include "kernel/Panic.h"
#include "lib/vitralKernel/Memory.h"
#include "lib/vitralKernel/String.h"

BlockDevice *BlockDevices::table[BlockDevices::MAX];
unsigned BlockDevices::total = 0;

void
BlockDevices::init() {
    if (!add(new LoopDevice("loop0", LoopDevice::imageStart(), LoopDevice::imageBytes()))) {
        Panic::halt("cannot register /dev/loop0");
    }
}

bool
BlockDevices::add(BlockDevice *device) {
    if (!device || total >= MAX || find(device->name())) {
        return false;
    }
    table[total++] = device;
    return true;
}

unsigned
BlockDevices::count() { return total; }

BlockDevice *
BlockDevices::at(unsigned index) { return index < total ? table[index] : nullptr; }

BlockDevice *
BlockDevices::find(const char *name) {
    for (unsigned i = 0; i < total; i++) {
        if (String::equals(table[i]->name(), name)) {
            return table[i];
        }
    }
    return nullptr;
}
