#include "drivers/usb/uhci/Uhci.h"
#include "drivers/usb/Usb.h"
#include "kernel/KernelLog.h"
#include "kernel/PhysicalMemory.h"
#include "lib/vitralKernel/Format.h"
#include "lib/vitralKernel/LockGuard.h"
#include "lib/vitralKernel/Memory.h"
#include "lib/vitralKernel/Ports.h"

static void
uhciBarrier() {
    asm volatile("" : : : "memory");
}

Uhci::Uhci(uint16_t ioBase, unsigned number)
    : io(ioBase), ports(0), memory(nullptr), frameList(nullptr), interruptSkeleton(nullptr),
      controlQh(nullptr), controlTds(nullptr), setupBuffer(nullptr), dataBuffer(nullptr), fault(false) {
    Format::print(nameText, sizeof(nameText), "UHCI %u", number);
    for (Pipe &pipe : pipes) {
        pipe.used = false;
    }
}

Uhci::~Uhci() {
    Ports::outw((uint16_t)(io + USBCMD), 0);
    if (memory) {
        PhysicalMemory::freeFrames((uint64_t)(uintptr_t)memory, PAGES);
    }
}

Uhci *
Uhci::create(const Pci::Location &at, unsigned number, unsigned &irq) {
    uint32_t bar = Pci::read32(at, 0x20);
    if (!(bar & 1) || (bar & 0xFFE0) == 0) {
        kprintf("USB: UHCI controller at PCI %u:%u.%u has no I/O ports\n", (uint32_t)at.bus, (uint32_t)at.device,
                (uint32_t)at.function);
        return nullptr;
    }
    uint16_t command = Pci::read16(at, Pci::COMMAND);
    command |= Pci::COMMAND_IO | Pci::COMMAND_BUS_MASTER;
    command &= ~Pci::COMMAND_INTERRUPT_DISABLE;
    Pci::write16(at, Pci::COMMAND, command);
    // Ends the BIOS keyboard and mouse emulation: no more SMIs on port 0x60/0x64 accesses.
    Pci::write16(at, PCI_LEGACY_SUPPORT, LEGACY_CLEAR);
    irq = (uint8_t)Pci::read32(at, Pci::INTERRUPT_LINE);

    Uhci *uhci = new Uhci((uint16_t)(bar & 0xFFE0), number);
    if (!uhci->initialize()) {
        kprintf("USB: UHCI controller at PCI %u:%u.%u does not start\n", (uint32_t)at.bus, (uint32_t)at.device,
                (uint32_t)at.function);
        delete uhci;
        return nullptr;
    }
    Pci::write16(at, PCI_LEGACY_SUPPORT, LEGACY_PIRQ);
    kprintf("USB: %s at PCI %u:%u.%u, I/O 0x%x, IRQ %u, %u ports\n", uhci->nameText, (uint32_t)at.bus,
            (uint32_t)at.device, (uint32_t)at.function, (uint32_t)uhci->io, irq, uhci->ports);
    return uhci;
}

bool
Uhci::initialize() {
    Ports::outw((uint16_t)(io + USBCMD), COMMAND_RESET);
    for (unsigned waited = 0; waited < 10 && (Ports::inw((uint16_t)(io + USBCMD)) & COMMAND_RESET); waited++) {
        Usb::delayMs(1);
    }
    if (Ports::inw((uint16_t)(io + USBCMD)) & COMMAND_RESET) {
        return false;
    }
    Ports::outw((uint16_t)(io + USBINTR), 0);
    Ports::outw((uint16_t)(io + USBCMD), 0);

    uint64_t address = PhysicalMemory::allocContiguous(PAGES, 1ull << 32);
    if (!address) {
        return false;
    }
    memory = (uint8_t *)(uintptr_t)address;
    Memory::set(memory, 0, PAGES * PAGE);
    frameList = (uint32_t *)memory;
    interruptSkeleton = (Qh *)(memory + PAGE);
    controlQh = (Qh *)(memory + PAGE + sizeof(Qh));
    controlTds = (Td *)(memory + PAGE + 64);
    setupBuffer = memory + 2 * PAGE;
    dataBuffer = memory + 2 * PAGE + 64;
    for (unsigned i = 0; i < MAX_PIPES; i++) {
        uint8_t *slot = memory + 2 * PAGE + PIPE_AREA + i * PIPE_STRIDE;
        pipes[i].qh = (Qh *)slot;
        pipes[i].td = (Td *)(slot + 32);
        pipes[i].buffer = slot + 64;
    }
    controlQh->link = LINK_TERMINATE;
    controlQh->element = LINK_TERMINATE;
    interruptSkeleton->link = physical(controlQh) | LINK_QH;
    interruptSkeleton->element = LINK_TERMINATE;
    for (unsigned i = 0; i < FRAMES; i++) {
        frameList[i] = physical(interruptSkeleton) | LINK_QH;
    }

    Ports::outl((uint16_t)(io + FRBASEADD), physical(frameList));
    Ports::outw((uint16_t)(io + FRNUM), 0);
    Ports::outb((uint16_t)(io + SOFMOD), 64);
    Ports::outw((uint16_t)(io + USBSTS), 0x3F);
    Ports::outw((uint16_t)(io + USBINTR), INTERRUPT_ON_COMPLETE | INTERRUPT_TIMEOUT_CRC);
    Ports::outw((uint16_t)(io + USBCMD), COMMAND_RUN | COMMAND_CONFIGURED | COMMAND_MAX_PACKET_64);
    Usb::delayMs(2);
    if (Ports::inw((uint16_t)(io + USBSTS)) & STATUS_HALTED) {
        return false;
    }
    while (ports < MAX_PORTS) {
        uint16_t status = Ports::inw(portRegister(ports + 1));
        if (!(status & PORT_ALWAYS_ONE) || status == 0xFFFF) {
            break;
        }
        ports++;
    }
    if (ports == 0 || ports == MAX_PORTS) {
        ports = 2;
    }
    return true;
}

UsbPortStatus
Uhci::portStatus(unsigned port) {
    uint16_t status = Ports::inw(portRegister(port));
    UsbPortStatus result;
    result.connected = status & PORT_CONNECTED;
    result.enabled = status & PORT_ENABLED;
    result.connectChanged = status & PORT_CONNECT_CHANGE;
    result.speed = (status & PORT_LOW_SPEED) ? UsbSpeed::Low : UsbSpeed::Full;
    return result;
}

void
Uhci::clearConnectChange(unsigned port) {
    uint16_t reg = portRegister(port);
    Ports::outw(reg, (uint16_t)((Ports::inw(reg) & PORT_WRITABLE) | PORT_CONNECT_CHANGE));
}

bool
Uhci::resetPort(unsigned port) {
    uint16_t reg = portRegister(port);
    Ports::outw(reg, (uint16_t)((Ports::inw(reg) & PORT_WRITABLE) | PORT_RESET));
    Usb::delayMs(50);
    Ports::outw(reg, (uint16_t)(Ports::inw(reg) & PORT_WRITABLE & ~PORT_RESET));
    Usb::delayMs(1);
    for (unsigned attempt = 0; attempt < 10; attempt++) {
        uint16_t status = Ports::inw(reg);
        if (!(status & PORT_CONNECTED)) {
            return false;
        }
        if (status & (PORT_CONNECT_CHANGE | PORT_ENABLE_CHANGE)) {
            Ports::outw(reg, (uint16_t)((status & PORT_WRITABLE) |
                                        (status & (PORT_CONNECT_CHANGE | PORT_ENABLE_CHANGE))));
        }
        if (status & PORT_ENABLED) {
            Usb::delayMs(10);
            return true;
        }
        Ports::outw(reg, (uint16_t)((status & PORT_WRITABLE) | PORT_ENABLED));
        Usb::delayMs(10);
    }
    return Ports::inw(reg) & PORT_ENABLED;
}

void
Uhci::disablePort(unsigned port) {
    uint16_t reg = portRegister(port);
    Ports::outw(reg, (uint16_t)(Ports::inw(reg) & PORT_WRITABLE & ~PORT_ENABLED));
}

uint32_t
Uhci::token(uint8_t pid, uint8_t address, uint8_t endpoint, unsigned toggle, unsigned length) {
    uint32_t encoded = length ? (uint32_t)(length - 1) : 0x7FF;
    return encoded << 21 | (uint32_t)(toggle & 1) << 19 | (uint32_t)(endpoint & 0x0F) << 15 |
           (uint32_t)(address & 0x7F) << 8 | pid;
}

int
Uhci::controlTransfer(uint8_t address, UsbSpeed speed, unsigned maxPacket, const UsbSetupPacket &setup, void *data) {
    unsigned length = setup.length;
    bool in = setup.requestType & 0x80;
    if (maxPacket == 0 || length > DATA_SIZE || (length + maxPacket - 1) / maxPacket + 2 > CONTROL_TDS) {
        return -1;
    }
    Memory::copy(setupBuffer, &setup, sizeof(setup));
    if (!in && length > 0 && data) {
        Memory::copy(dataBuffer, data, length);
    }
    uint32_t base = TD_ACTIVE | TD_ERROR_LIMIT | (speed == UsbSpeed::Low ? TD_LOW_SPEED : 0);
    Td *td = controlTds;
    unsigned count = 0;
    td[count].status = base;
    td[count].token = token(PID_SETUP, address, 0, 0, sizeof(setup));
    td[count].buffer = physical(setupBuffer);
    count++;
    unsigned toggle = 1;
    for (unsigned offset = 0; offset < length; offset += maxPacket) {
        unsigned chunk = length - offset < maxPacket ? length - offset : maxPacket;
        td[count].status = base | (in ? TD_SHORT_PACKET : 0);
        td[count].token = token(in ? PID_IN : PID_OUT, address, 0, toggle, chunk);
        td[count].buffer = physical(dataBuffer + offset);
        toggle ^= 1;
        count++;
    }
    unsigned statusStage = count;
    td[count].status = base | TD_INTERRUPT;
    td[count].token = token(in && length > 0 ? PID_OUT : PID_IN, address, 0, 1, 0);
    td[count].buffer = 0;
    count++;
    for (unsigned i = 0; i < count; i++) {
        td[i].link = i + 1 < count ? physical(&td[i + 1]) | LINK_DEPTH_FIRST : LINK_TERMINATE;
    }
    uhciBarrier();
    controlQh->element = physical(td);

    uint64_t deadline = Usb::nowMs() + CONTROL_TIMEOUT_MS;
    int result = -1;
    bool redirected = false;
    for (;;) {
        unsigned i = 0;
        bool failed = false;
        bool shortPacket = false;
        for (; i < count; i++) {
            uint32_t status = td[i].status;
            if (status & TD_ACTIVE) {
                break;
            }
            if (status & TD_ERRORS) {
                failed = true;
                break;
            }
            if (in && i > 0 && i < statusStage && actualLength(status) < expectedLength(td[i].token)) {
                shortPacket = true;
                break;
            }
        }
        if (failed) {
            break;
        }
        bool done = i == count;
        if (shortPacket) {
            if (!redirected) {
                controlQh->element = physical(&td[statusStage]);
                redirected = true;
            }
            uint32_t status = td[statusStage].status;
            if (status & TD_ERRORS) {
                break;
            }
            done = !(status & TD_ACTIVE);
        }
        if (done) {
            result = 0;
            for (unsigned k = 1; k < statusStage; k++) {
                uint32_t status = td[k].status;
                if (status & TD_ACTIVE) {
                    break;
                }
                unsigned moved = actualLength(status);
                result += (int)moved;
                if (moved < expectedLength(td[k].token)) {
                    break;
                }
            }
            break;
        }
        if (Usb::nowMs() > deadline) {
            break;
        }
        asm volatile("pause");
    }
    if (result < 0 || redirected) {
        controlQh->element = LINK_TERMINATE;
        Usb::delayMs(2);
    }
    if (result > 0 && in && data) {
        Memory::copy(data, dataBuffer, (size_t)result);
    }
    return result;
}

void
Uhci::arm(Pipe &pipe) {
    pipe.td->link = LINK_TERMINATE;
    pipe.td->buffer = physical(pipe.buffer);
    pipe.td->token = token(PID_IN, pipe.address, pipe.endpoint, pipe.toggle, pipe.maxPacket);
    pipe.td->status = TD_ACTIVE | TD_ERROR_LIMIT | TD_INTERRUPT | (pipe.lowSpeed ? TD_LOW_SPEED : 0);
    uhciBarrier();
    pipe.qh->element = physical(pipe.td);
}

void *
Uhci::openInterruptPipe(uint8_t address, UsbSpeed speed, uint8_t endpoint, unsigned maxPacket,
                        UsbReportHandler handler, void *context) {
    LockGuard guard(lock);
    for (Pipe &pipe : pipes) {
        if (pipe.used) {
            continue;
        }
        pipe.address = address;
        pipe.endpoint = endpoint;
        pipe.toggle = 0;
        pipe.lowSpeed = speed == UsbSpeed::Low;
        pipe.maxPacket = maxPacket == 0 || maxPacket > PIPE_BUFFER ? PIPE_BUFFER : maxPacket;
        pipe.errors = 0;
        pipe.dead = false;
        pipe.handler = handler;
        pipe.context = context;
        pipe.qh->element = LINK_TERMINATE;
        arm(pipe);
        pipe.qh->link = interruptSkeleton->link;
        uhciBarrier();
        interruptSkeleton->link = physical(pipe.qh) | LINK_QH;
        pipe.used = true;
        return &pipe;
    }
    return nullptr;
}

void
Uhci::closeInterruptPipe(void *handle) {
    Pipe *pipe = (Pipe *)handle;
    if (!pipe) {
        return;
    }
    {
        LockGuard guard(lock);
        pipe->used = false;
        Qh *previous = interruptSkeleton;
        while (previous != controlQh && (previous->link & ~0xFu) != physical(pipe->qh)) {
            previous = (Qh *)(uintptr_t)(previous->link & ~0xFu);
        }
        if (previous != controlQh) {
            previous->link = pipe->qh->link;
        }
        pipe->qh->element = LINK_TERMINATE;
    }
    // The controller may still hold the queue head for the frame in progress.
    Usb::delayMs(2);
}

bool
Uhci::poll() {
    uint16_t status = Ports::inw((uint16_t)(io + USBSTS));
    if (status == 0xFFFF) {
        return false;
    }
    bool pending = status & (STATUS_INTERRUPT | STATUS_ERROR | STATUS_SYSTEM_ERROR | STATUS_PROCESS_ERROR);
    if (status & 0x1F) {
        Ports::outw((uint16_t)(io + USBSTS), (uint16_t)(status & 0x1F));
    }
    if (status & (STATUS_SYSTEM_ERROR | STATUS_PROCESS_ERROR)) {
        fault = true;
    }
    LockGuard guard(lock);
    for (Pipe &pipe : pipes) {
        if (!pipe.used || pipe.dead) {
            continue;
        }
        uint32_t td = pipe.td->status;
        if (td & TD_ACTIVE) {
            continue;
        }
        if (td & TD_ERRORS) {
            if (++pipe.errors >= 3) {
                pipe.dead = true;
                continue;
            }
        } else {
            pipe.errors = 0;
            pipe.toggle ^= 1;
            pipe.handler(pipe.context, pipe.buffer, actualLength(td));
        }
        arm(pipe);
    }
    return pending;
}

bool
Uhci::takeFault() {
    if (!fault) {
        return false;
    }
    fault = false;
    return true;
}
