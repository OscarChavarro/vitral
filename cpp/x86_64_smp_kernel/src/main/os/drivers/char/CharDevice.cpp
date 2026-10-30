#include "drivers/char/CharDevice.h"
#include "lib/vitralKernel/Memory.h"
#include "lib/vitralKernel/String.h"
#include "drivers/char/CharDevices.h"

CharDevice::CharDevice(const char *deviceDirectory, const char *deviceName) {
    String::copy(directoryBuffer, deviceDirectory ? deviceDirectory : "", NAME_LENGTH);
    String::copy(nameBuffer, deviceName, NAME_LENGTH);
}
