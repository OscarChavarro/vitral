#ifndef __USB__
#define __USB__
#include <stdint.h>
#include "drivers/usb/UsbDevice.h"
#include "drivers/usb/UsbHostController.h"
#include "kernel/TrapFrame.h"

class Process;

// The USB core. takeOver() finds the host controllers on PCI and takes them from the BIOS, which stops its PS/2
// emulation of USB keyboards and mice; it must run before the PS/2 devices are probed. start() enumerates what is
// attached, once the clock runs, and startMonitor() then follows connections and disconnections from a kernel thread
// (a KVM switch detaches its keyboard and mouse when it selects another computer).
//
// Supported: UHCI controllers, and EHCI controllers by routing their ports to their UHCI companions (full and low
// speed only); external hubs; HID keyboards and mice in boot protocol.
class Usb {
public:
    static constexpr uint8_t DESCRIPTOR_DEVICE = 1;
    static constexpr uint8_t DESCRIPTOR_CONFIGURATION = 2;
    static constexpr uint8_t DESCRIPTOR_STRING = 3;
    static constexpr uint8_t DESCRIPTOR_INTERFACE = 4;
    static constexpr uint8_t DESCRIPTOR_ENDPOINT = 5;
    static constexpr uint8_t REQUEST_GET_STATUS = 0;
    static constexpr uint8_t REQUEST_CLEAR_FEATURE = 1;
    static constexpr uint8_t REQUEST_SET_FEATURE = 3;
    static constexpr uint8_t REQUEST_SET_ADDRESS = 5;
    static constexpr uint8_t REQUEST_GET_DESCRIPTOR = 6;
    static constexpr uint8_t REQUEST_SET_CONFIGURATION = 9;

    static void takeOver();
    static void start();
    static void startMonitor(Process *creator);
    // From the clock interrupt: services the controllers (also when their interrupt line is unusable) and repeats
    // held USB keys.
    static void timer();

    static uint64_t nowMs();
    static void delayMs(unsigned milliseconds);

    // Enumerates the device behind a port that was just reset and enabled; false when it does not answer.
    static bool attach(UsbHostController &controller, UsbDevice *parent, unsigned port, UsbSpeed speed);
    // Forgets the devices behind a port, and theirs when one is a hub.
    static void detach(UsbHostController &controller, UsbDevice *parent, unsigned port);
    static unsigned keyboardCount() { return keyboards; }
    static unsigned mouseCount() { return mice; }

private:
    static constexpr unsigned MAX_CONTROLLERS = 8;
    static constexpr unsigned MAX_DEVICES = 32;
    static constexpr unsigned MONITOR_MS = 250;
    static constexpr unsigned CONFIGURATION_MAX = 512;

    static UsbHostController *controllers[MAX_CONTROLLERS];
    static unsigned controllerCount;
    static UsbDevice devices[MAX_DEVICES];
    static bool rootConnected[MAX_CONTROLLERS][8];
    static bool threaded;
    static bool started;
    static unsigned keyboards;
    static unsigned mice;

    static void addController(UsbHostController *controller, unsigned irq);
    static TrapFrame *onIrq(TrapFrame *frame);
    static void scanRootPorts(UsbHostController &controller);
    static void scanHubs();
    static void monitor(void *argument);
    static void pitDelayMs(unsigned milliseconds);
    static UsbDevice *allocateDevice();
    static void readName(UsbDevice &device, uint8_t manufacturerIndex, uint8_t productIndex);
    static bool readString(UsbDevice &device, uint8_t index, uint16_t language, char *out, unsigned size);
    static void bindInterfaces(UsbDevice &device, const uint8_t *configuration, unsigned length);
    static void release(UsbDevice &device);
};

#endif
