#include "drivers/usb/Usb.h"
#include "drivers/hid/usbKeyboard/UsbKeyboard.h"
#include "drivers/hid/usbMouse/UsbMouse.h"
#include "drivers/pci/Pci.h"
#include "drivers/usb/UsbHub.h"
#include "drivers/usb/ehci/Ehci.h"
#include "drivers/usb/uhci/Uhci.h"
#include "kernel/Clock.h"
#include "kernel/Interrupts.h"
#include "kernel/KernelLog.h"
#include "kernel/Pic.h"
#include "kernel/Process.h"
#include "kernel/Scheduler.h"
#include "lib/vitralKernel/CpuMask.h"
#include "lib/vitralKernel/Format.h"
#include "lib/vitralKernel/Ports.h"
#include "syscalls/Syscalls.h"

UsbHostController *Usb::controllers[Usb::MAX_CONTROLLERS];
unsigned Usb::controllerCount = 0;
UsbDevice Usb::devices[Usb::MAX_DEVICES];
bool Usb::rootConnected[Usb::MAX_CONTROLLERS][8];
bool Usb::threaded = false;
bool Usb::started = false;
unsigned Usb::keyboards = 0;
unsigned Usb::mice = 0;

static constexpr uint8_t PCI_CLASS_SERIAL_BUS = 0x0C;
static constexpr uint8_t PCI_SUBCLASS_USB = 0x03;
static constexpr uint8_t INTERFACE_UHCI = 0x00;
static constexpr uint8_t INTERFACE_OHCI = 0x10;
static constexpr uint8_t INTERFACE_EHCI = 0x20;
static constexpr uint8_t INTERFACE_XHCI = 0x30;
static constexpr uint8_t CLASS_HID = 3;
static constexpr uint8_t CLASS_HUB = 9;
static constexpr uint8_t SUBCLASS_BOOT = 1;
static constexpr uint8_t PROTOCOL_KEYBOARD = 1;
static constexpr uint8_t PROTOCOL_MOUSE = 2;
static constexpr size_t MONITOR_STACK = 32768;

void
Usb::takeOver() {
    Pci::Location at = {0, 0, 0};
    for (bool first = true; Pci::findClass(PCI_CLASS_SERIAL_BUS, PCI_SUBCLASS_USB, first, at); first = false) {
        if (Pci::read8(at, Pci::PROGRAMMING_INTERFACE) == INTERFACE_EHCI) {
            Ehci::release(at);
        }
    }
    for (bool first = true; Pci::findClass(PCI_CLASS_SERIAL_BUS, PCI_SUBCLASS_USB, first, at); first = false) {
        uint8_t kind = Pci::read8(at, Pci::PROGRAMMING_INTERFACE);
        if (kind == INTERFACE_UHCI) {
            unsigned irq = 0;
            Uhci *uhci = Uhci::create(at, controllerCount, irq);
            if (uhci && controllerCount < MAX_CONTROLLERS) {
                addController(uhci, irq);
            } else if (uhci) {
                delete uhci;
            }
        } else if (kind == INTERFACE_OHCI || kind == INTERFACE_XHCI) {
            kprintf("USB: %s controller at PCI %u:%u.%u is not supported\n", kind == INTERFACE_OHCI ? "OHCI" : "xHCI",
                    (uint32_t)at.bus, (uint32_t)at.device, (uint32_t)at.function);
        }
    }
    if (controllerCount == 0) {
        kprintf("USB: no supported host controller\n");
    }
}

void
Usb::addController(UsbHostController *controller, unsigned irq) {
    controller->setNumber(controllerCount);
    controllers[controllerCount++] = controller;
    if (irq < 16 && Interrupts::addSharedIrqHandler(irq, onIrq)) {
        Pic::unmask(irq);
    } else {
        kprintf("USB: %s has no usable interrupt line, serviced at each clock tick\n", controller->name());
    }
}

TrapFrame *
Usb::onIrq(TrapFrame *frame) {
    for (unsigned i = 0; i < controllerCount; i++) {
        controllers[i]->poll();
    }
    return frame;
}

void
Usb::timer() {
    for (unsigned i = 0; i < controllerCount; i++) {
        controllers[i]->poll();
    }
    UsbKeyboard::repeatTick();
}

uint64_t
Usb::nowMs() { return Clock::nanoseconds() / 1000000; }

// PIT channel 2 in mode 0, as Uart uses it: works before the clock interrupt runs.
void
Usb::pitDelayMs(unsigned milliseconds) {
    while (milliseconds > 0) {
        unsigned chunk = milliseconds > 50 ? 50 : milliseconds;
        unsigned count = chunk * 1193;
        uint8_t control = Ports::inb(0x61);
        Ports::outb(0x61, (uint8_t)((control & ~0x02) | 0x01));
        Ports::outb(0x43, 0xB0);
        Ports::outb(0x42, (uint8_t)(count & 0xFF));
        Ports::outb(0x42, (uint8_t)(count >> 8));
        while (!(Ports::inb(0x61) & 0x20)) {
            asm volatile("pause");
        }
        milliseconds -= chunk;
    }
}

void
Usb::delayMs(unsigned milliseconds) {
    if (threaded && Scheduler::isRunning()) {
        Syscalls::sleep(milliseconds);
    } else if (Clock::ticks() > 19) {
        uint64_t end = Clock::nanoseconds() + (uint64_t)milliseconds * 1000000;
        while (Clock::nanoseconds() < end) {
            asm volatile("pause");
        }
    } else {
        pitDelayMs(milliseconds);
    }
}

void
Usb::start() {
    if (controllerCount == 0) {
        return;
    }
    delayMs(100);
    for (unsigned i = 0; i < controllerCount; i++) {
        scanRootPorts(*controllers[i]);
    }
    started = true;
    kprintf("USB: %u keyboard%s and %u %s found\n", keyboards, keyboards == 1 ? "" : "s", mice,
            mice == 1 ? "mouse" : "mice");
}

void
Usb::startMonitor(Process *creator) {
    if (controllerCount == 0 || !creator) {
        return;
    }
    Process *thread = Scheduler::spawnThread(monitor, nullptr, MONITOR_STACK, creator, CpuMask::only(0));
    if (thread) {
        thread->setName("usb");
    } else {
        kprintf("USB: cannot start the monitor thread, hot plugging is off\n");
    }
}

// Pinned to CPU 0, where the PIC delivers the controllers' interrupts: the controller locks then suffice to keep the
// interrupt handlers out while the schedule changes.
void
Usb::monitor(void *) {
    threaded = true;
    for (;;) {
        Syscalls::sleep(MONITOR_MS);
        for (unsigned i = 0; i < controllerCount; i++) {
            if (controllers[i]->takeFault()) {
                kprintf("USB: %s stopped on a host controller error\n", controllers[i]->name());
            }
            scanRootPorts(*controllers[i]);
        }
        scanHubs();
    }
}

void
Usb::scanRootPorts(UsbHostController &controller) {
    unsigned n = controller.number();
    unsigned ports = controller.portCount();
    for (unsigned port = 1; port <= ports && port < 8; port++) {
        UsbPortStatus status = controller.portStatus(port);
        if (status.connectChanged) {
            controller.clearConnectChange(port);
        }
        if (rootConnected[n][port] && (!status.connected || status.connectChanged)) {
            detach(controller, nullptr, port);
            rootConnected[n][port] = false;
        }
        if (status.connected && !rootConnected[n][port]) {
            rootConnected[n][port] = true;
            delayMs(100);
            if (!controller.resetPort(port)) {
                kprintf("USB: %s port %u does not enable after reset\n", controller.name(), port);
                continue;
            }
            attach(controller, nullptr, port, controller.portStatus(port).speed);
        }
    }
}

void
Usb::scanHubs() {
    for (UsbDevice &device : devices) {
        if (device.inUse && device.hub) {
            device.hub->scan();
        }
    }
}

UsbDevice *
Usb::allocateDevice() {
    for (UsbDevice &device : devices) {
        if (!device.inUse) {
            device.controller = nullptr;
            device.parent = nullptr;
            device.port = 0;
            device.address = 0;
            device.speed = UsbSpeed::Full;
            device.maxPacket0 = 8;
            device.vendor = 0;
            device.product = 0;
            device.deviceClass = 0;
            device.hub = nullptr;
            for (UsbInterfaceDriver *&driver : device.drivers) {
                driver = nullptr;
            }
            device.name[0] = 0;
            device.inUse = true;
            return &device;
        }
    }
    return nullptr;
}

static void
describeUsbLocation(char *out, size_t size, const UsbHostController &controller, const UsbDevice *parent,
                    unsigned port) {
    if (parent) {
        Format::print(out, size, "hub %u port %u", (uint32_t)parent->address, port);
    } else {
        Format::print(out, size, "%s port %u", controller.name(), port);
    }
}

bool
Usb::attach(UsbHostController &controller, UsbDevice *parent, unsigned port, UsbSpeed speed) {
    char where[40];
    describeUsbLocation(where, sizeof(where), controller, parent, port);
    UsbDevice *device = allocateDevice();
    if (!device) {
        kprintf("USB: too many devices, ignoring the one at %s\n", where);
        return false;
    }
    device->controller = &controller;
    device->parent = parent;
    device->port = (uint8_t)port;
    device->speed = speed;

    uint8_t descriptor[18];
    int got = -1;
    for (unsigned attempt = 0; attempt < 3 && got < 8; attempt++) {
        got = device->control(0x80, REQUEST_GET_DESCRIPTOR, DESCRIPTOR_DEVICE << 8, 0, 8, descriptor);
        if (got < 8) {
            delayMs(20);
        }
    }
    if (got < 8) {
        kprintf("USB: the device at %s does not answer\n", where);
        device->inUse = false;
        return false;
    }
    uint8_t packet = descriptor[7];
    device->maxPacket0 = (packet == 8 || packet == 16 || packet == 32 || packet == 64) ? packet : 8;
    uint8_t address = controller.allocateAddress();
    if (!address || device->control(0x00, REQUEST_SET_ADDRESS, address, 0, 0, nullptr) < 0) {
        kprintf("USB: the device at %s does not take an address\n", where);
        controller.releaseAddress(address);
        device->inUse = false;
        return false;
    }
    device->address = address;
    delayMs(10);

    uint8_t configuration[CONFIGURATION_MAX];
    bool described = device->control(0x80, REQUEST_GET_DESCRIPTOR, DESCRIPTOR_DEVICE << 8, 0, 18, descriptor) == 18 &&
                     device->control(0x80, REQUEST_GET_DESCRIPTOR, DESCRIPTOR_CONFIGURATION << 8, 0, 9,
                                     configuration) == 9;
    int total = described ? (configuration[2] | configuration[3] << 8) : 0;
    if (total > (int)CONFIGURATION_MAX) {
        total = CONFIGURATION_MAX;
    }
    if (described && total >= 9) {
        total = device->control(0x80, REQUEST_GET_DESCRIPTOR, DESCRIPTOR_CONFIGURATION << 8, 0, (uint16_t)total,
                                configuration);
    }
    if (!described || total < 9 ||
        device->control(0x00, REQUEST_SET_CONFIGURATION, configuration[5], 0, 0, nullptr) < 0) {
        kprintf("USB: the device at %s cannot be configured\n", where);
        controller.releaseAddress(address);
        device->inUse = false;
        return false;
    }
    device->vendor = (uint16_t)(descriptor[8] | descriptor[9] << 8);
    device->product = (uint16_t)(descriptor[10] | descriptor[11] << 8);
    device->deviceClass = descriptor[4];
    readName(*device, descriptor[14], descriptor[15]);
    kprintf("USB: %04x:%04x \"%s\" at %s, %s speed, address %u\n", (uint32_t)device->vendor,
            (uint32_t)device->product, device->name, where, device->speedName(), (uint32_t)address);
    bindInterfaces(*device, configuration, (unsigned)total);
    return true;
}

bool
Usb::readString(UsbDevice &device, uint8_t index, uint16_t language, char *out, unsigned size) {
    uint8_t buffer[255];
    int got = device.control(0x80, REQUEST_GET_DESCRIPTOR, (uint16_t)(DESCRIPTOR_STRING << 8 | index), language,
                             sizeof(buffer), buffer);
    if (got < 2 || buffer[1] != DESCRIPTOR_STRING) {
        return false;
    }
    unsigned length = buffer[0] < got ? buffer[0] : (unsigned)got;
    unsigned n = 0;
    for (unsigned i = 2; i + 1 < length && n + 1 < size; i += 2) {
        uint16_t c = (uint16_t)(buffer[i] | buffer[i + 1] << 8);
        out[n++] = (c >= 0x20 && c < 0x7F) ? (char)c : '?';
    }
    while (n > 0 && out[n - 1] == ' ') {
        n--;
    }
    out[n] = 0;
    return n > 0;
}

void
Usb::readName(UsbDevice &device, uint8_t manufacturerIndex, uint8_t productIndex) {
    device.name[0] = 0;
    uint8_t languages[4];
    if ((!productIndex && !manufacturerIndex) ||
        device.control(0x80, REQUEST_GET_DESCRIPTOR, DESCRIPTOR_STRING << 8, 0, sizeof(languages), languages) < 4) {
        Format::print(device.name, sizeof(device.name), "unnamed");
        return;
    }
    uint16_t language = (uint16_t)(languages[2] | languages[3] << 8);
    if (!(productIndex && readString(device, productIndex, language, device.name, sizeof(device.name))) &&
        !(manufacturerIndex && readString(device, manufacturerIndex, language, device.name, sizeof(device.name)))) {
        Format::print(device.name, sizeof(device.name), "unnamed");
    }
}

void
Usb::bindInterfaces(UsbDevice &device, const uint8_t *configuration, unsigned length) {
    unsigned bound = 0;
    int interfaceNumber = -1;
    uint8_t interfaceClass = 0;
    uint8_t interfaceSubclass = 0;
    uint8_t interfaceProtocol = 0;
    bool handled = true;
    bool hub = device.deviceClass == CLASS_HUB;
    for (unsigned at = 0; at + 2 <= length;) {
        uint8_t size = configuration[at];
        uint8_t type = configuration[at + 1];
        if (size < 2 || at + size > length) {
            break;
        }
        if (type == DESCRIPTOR_INTERFACE && size >= 9) {
            interfaceNumber = configuration[at + 2];
            interfaceClass = configuration[at + 5];
            interfaceSubclass = configuration[at + 6];
            interfaceProtocol = configuration[at + 7];
            handled = configuration[at + 3] != 0;
            hub = hub || interfaceClass == CLASS_HUB;
        } else if (type == DESCRIPTOR_ENDPOINT && size >= 7 && !handled && interfaceNumber >= 0) {
            uint8_t endpoint = configuration[at + 2];
            uint8_t attributes = configuration[at + 3];
            unsigned maxPacket = (configuration[at + 4] | configuration[at + 5] << 8) & 0x7FF;
            bool interruptIn = (endpoint & 0x80) && (attributes & 3) == 3;
            if (interruptIn && interfaceClass == CLASS_HID && interfaceSubclass == SUBCLASS_BOOT &&
                bound < UsbDevice::MAX_DRIVERS) {
                UsbInterfaceDriver *driver = nullptr;
                if (interfaceProtocol == PROTOCOL_KEYBOARD) {
                    driver = UsbKeyboard::attach(device, (uint8_t)interfaceNumber, endpoint & 0x0F, maxPacket);
                    keyboards += driver ? 1 : 0;
                } else if (interfaceProtocol == PROTOCOL_MOUSE) {
                    driver = UsbMouse::attach(device, (uint8_t)interfaceNumber, endpoint & 0x0F, maxPacket);
                    mice += driver ? 1 : 0;
                }
                if (driver) {
                    device.drivers[bound++] = driver;
                }
            }
            handled = interruptIn || handled;
        }
        at += size;
    }
    if (hub && bound < UsbDevice::MAX_DRIVERS) {
        device.hub = UsbHub::attach(device);
        if (device.hub) {
            device.drivers[bound++] = device.hub;
            device.hub->scan();
        }
    }
    if (bound == 0) {
        kprintf("USB: no driver for \"%s\" (%04x:%04x)\n", device.name, (uint32_t)device.vendor,
                (uint32_t)device.product);
    }
}

void
Usb::release(UsbDevice &device) {
    kprintf("USB: \"%s\" at address %u disconnected\n", device.name, (uint32_t)device.address);
    for (UsbInterfaceDriver *&driver : device.drivers) {
        if (driver) {
            const char *kind = driver->kind();
            if (kind[0] == 'k' && keyboards > 0) {
                keyboards--;
            } else if (kind[0] == 'm' && mice > 0) {
                mice--;
            }
            delete driver;
            driver = nullptr;
        }
    }
    device.hub = nullptr;
    device.controller->releaseAddress(device.address);
    device.inUse = false;
}

void
Usb::detach(UsbHostController &controller, UsbDevice *parent, unsigned port) {
    for (UsbDevice &device : devices) {
        if (device.inUse && device.controller == &controller && device.parent == parent && device.port == port) {
            release(device);
        }
    }
}
