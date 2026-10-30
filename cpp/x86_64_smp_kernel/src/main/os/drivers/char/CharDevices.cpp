#include "drivers/char/CharDevices.h"
#include "drivers/char/CharDevice.h"
#include "lib/vitralKernel/Memory.h"
#include "lib/vitralKernel/String.h"

CharDevice *CharDevices::table[CharDevices::MAX];
unsigned CharDevices::total = 0;

bool
CharDevices::add(CharDevice *device) {
    if (!device || total >= MAX) {
        return false;
    }
    table[total++] = device;
    return true;
}

unsigned
CharDevices::count() { return total; }

CharDevice *
CharDevices::at(unsigned index) { return index < total ? table[index] : nullptr; }
