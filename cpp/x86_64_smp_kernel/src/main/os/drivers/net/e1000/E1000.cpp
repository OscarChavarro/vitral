#include "drivers/net/e1000/E1000.h"
#include "fs/Err.h"
#include "kernel/KernelHeap.h"
#include "kernel/Interrupts.h"
#include "kernel/LocalApic.h"
#include "kernel/Paging.h"
#include "kernel/Pic.h"
#include "kernel/KernelLog.h"
#include "lib/vitralKernel/Format.h"
#include "net/Ethernet.h"
#include "net/Net.h"
#include "lib/vitralKernel/LockGuard.h"
#include "net/NetDevices.h"

E1000 *E1000::instance = nullptr;

E1000::E1000(volatile uint8_t *registers, uint8_t irqLine, bool pchDevice)
    : NetDevice("eth0", MacAddress()), mmio(registers), irq(irqLine), msi(false), pch(pchDevice), rxRing(nullptr),
      txRing(nullptr), rxBuffers(nullptr), txBuffers(nullptr), rxNext(0), txNext(0) {}

bool
E1000::init() {
    static const uint16_t DEVICES[] = {0x100E, 0x100F, 0x1004, 0x15B9};
    Pci::Location at;
    bool found = false;
    for (uint16_t device : DEVICES) {
        if (Pci::find(0x8086, device, at)) {
            found = true;
            break;
        }
    }
    if (!found) {
        return false;
    }

    uint64_t address;
    uint64_t size;
    if (!Pci::memoryBar(at, 0, address, size) || address + size > Paging::mappedBytes()) {
        kprintf("Network: e1000 found but its register BAR is unusable\n");
        return false;
    }
    bool pch = Pci::read16(at, Pci::DEVICE_ID) == 0x15B9;
    uint8_t line = (uint8_t)Pci::read32(at, Pci::INTERRUPT_LINE);
    if (!pch && line >= 16) {
        kprintf("Network: e1000 has no PIC interrupt line (%u)\n", (uint32_t)line);
        return false;
    }
    uint16_t command = Pci::read16(at, Pci::COMMAND);
    command |= Pci::COMMAND_MEMORY | Pci::COMMAND_BUS_MASTER;
    command &= ~Pci::COMMAND_INTERRUPT_DISABLE;
    Pci::write16(at, Pci::COMMAND, command);

    E1000 *card = new E1000((volatile uint8_t *)address, line, pch);
    MacAddress mac;
    if (!card->reset() || !card->readMac(mac) || !card->allocateRings()) {
        kprintf("Network: e1000 initialisation failed\n");
        delete card;
        return false;
    }
    card->setMac(mac);
    instance = card;
    unsigned vector = pch ? MSI_VECTOR : Interrupts::IRQ_BASE + line;
    if (!Interrupts::registerHandler(vector, onIrq)) {
        kprintf("Network: e1000 interrupt vector already taken\n");
        instance = nullptr;
        delete card;
        return false;
    }
    if (pch) {
        if (!LocalApic::supported()) {
            kprintf("Network: I219 requires a local APIC for MSI\n");
            Interrupts::unregisterHandler(vector);
            instance = nullptr;
            delete card;
            return false;
        }
        LocalApic::enable();
        if (!Pci::enableMsi(at, MSI_VECTOR, LocalApic::id())) {
            kprintf("Network: I219 has no usable MSI capability\n");
            Interrupts::unregisterHandler(vector);
            instance = nullptr;
            delete card;
            return false;
        }
        card->msi = true;
    }
    NetDevices::add(card);

    char text[MacAddress::TEXT_SIZE];
    mac.format(text);
    char speed[16] = "";
    if (card->linkUp()) {
        Format::print(speed, sizeof(speed), " %u Mb/s", card->linkMbps());
    }
    kprintf("Network: eth0 Intel %s (PCI %u:%u.%u) MAC %s %s %u, link %s%s\n", pch ? "I219-LM" : "e1000",
            (uint32_t)at.bus, (uint32_t)at.device, (uint32_t)at.function, text, pch ? "MSI vector" : "IRQ",
            (uint32_t)(pch ? MSI_VECTOR : line), card->linkUp() ? "up" : "down", speed);
    return true;
}

bool
E1000::reset() {
    if (pch) {
        return resetPch();
    }
    write(IMC, 0xFFFFFFFFu);
    write(RCTL, 0);
    write(TCTL, TCTL_PSP);
    write(CTRL, read(CTRL) | CTRL_RST);
    for (unsigned spin = 0; read(CTRL) & CTRL_RST; spin++) {
        if (spin > 1000000) {
            return false;
        }
    }
    write(IMC, 0xFFFFFFFFu);
    read(ICR);
    write(CTRL, (read(CTRL) | CTRL_SLU | CTRL_ASDE) & ~(CTRL_LRST | CTRL_ILOS | CTRL_PHY_RST));
    return true;
}

bool
E1000::resetPch() {
    write(IMC, 0xFFFFFFFFu);
    write(RCTL, 0);
    write(TCTL, TCTL_PSP);
    uint32_t control = read(CTRL) & ~CTRL_PHY_RST;
    write(CTRL, control | CTRL_RST);
    for (unsigned spin = 0; read(CTRL) & CTRL_RST; spin++) {
        if (spin > 1000000) {
            return false;
        }
    }
    for (unsigned spin = 0; spin < 10000000; spin++) {
        if (read(STATUS) & STATUS_LAN_INIT_DONE) {
            break;
        }
        asm volatile("pause");
    }
    write(IMC, 0xFFFFFFFFu);
    read(ICR);
    write(CTRL_EXT, read(CTRL_EXT) | CTRL_EXT_DRV_LOAD);
    write(CTRL, (read(CTRL) | CTRL_SLU | CTRL_ASDE | CTRL_MEHE) & ~(CTRL_LRST | CTRL_ILOS));
    return true;
}

bool
E1000::readEeprom(uint8_t word, uint16_t &value) {
    write(EERD, EERD_START | (uint32_t)word << 8);
    for (unsigned spin = 0; spin < 1000000; spin++) {
        uint32_t v = read(EERD);
        if (v & EERD_DONE) {
            value = (uint16_t)(v >> 16);
            return true;
        }
    }
    return false;
}

bool
E1000::readMac(MacAddress &out) {
    uint8_t raw[6];
    uint32_t low = read(RAL0);
    uint32_t high = read(RAH0);
    for (unsigned i = 0; i < 4; i++) {
        raw[i] = (uint8_t)(low >> (i * 8));
    }
    raw[4] = (uint8_t)high;
    raw[5] = (uint8_t)(high >> 8);
    out = MacAddress::from(raw);
    if (out.isZero() || out.isMulticast()) {
        for (unsigned i = 0; i < 3; i++) {
            uint16_t word;
            if (!readEeprom((uint8_t)i, word)) {
                return false;
            }
            raw[i * 2] = (uint8_t)word;
            raw[i * 2 + 1] = (uint8_t)(word >> 8);
        }
        out = MacAddress::from(raw);
        if (out.isZero() || out.isMulticast()) {
            return false;
        }
    }
    write(RAL0, (uint32_t)raw[0] | (uint32_t)raw[1] << 8 | (uint32_t)raw[2] << 16 | (uint32_t)raw[3] << 24);
    write(RAH0, (uint32_t)raw[4] | (uint32_t)raw[5] << 8 | RAH_AV);
    return true;
}

bool
E1000::allocateRings() {
    rxRing = (volatile RxDescriptor *)KernelHeap::tryAllocateAligned(RX_COUNT * sizeof(RxDescriptor), 128);
    txRing = (volatile TxDescriptor *)KernelHeap::tryAllocateAligned(TX_COUNT * sizeof(TxDescriptor), 128);
    rxBuffers = (uint8_t *)KernelHeap::tryAllocateAligned(RX_COUNT * BUFFER_SIZE, 16);
    txBuffers = (uint8_t *)KernelHeap::tryAllocateAligned(TX_COUNT * BUFFER_SIZE, 16);
    if (!rxRing || !txRing || !rxBuffers || !txBuffers) {
        return false;
    }
    for (unsigned i = 0; i < RX_COUNT; i++) {
        rxRing[i].address = (uint64_t)(rxBuffers + i * BUFFER_SIZE);
        rxRing[i].status = 0;
        rxRing[i].errors = 0;
        rxRing[i].length = 0;
    }
    for (unsigned i = 0; i < TX_COUNT; i++) {
        txRing[i].address = (uint64_t)(txBuffers + i * BUFFER_SIZE);
        txRing[i].cmd = 0;
        txRing[i].status = TX_DD;
        txRing[i].length = 0;
    }
    return true;
}

int
E1000::start() {
    rxNext = txNext = 0;
    for (unsigned i = 0; i < RX_COUNT; i++) {
        rxRing[i].status = 0;
    }
    for (unsigned i = 0; i < TX_COUNT; i++) {
        txRing[i].status = TX_DD;
    }
    for (unsigned i = 0; i < 128; i++) {
        write(MTA + i * 4, 0);
    }

    uint64_t rx = (uint64_t)rxRing;
    uint64_t tx = (uint64_t)txRing;
    write(RDBAL, (uint32_t)rx);
    write(RDBAH, (uint32_t)(rx >> 32));
    write(RDLEN, RX_COUNT * sizeof(RxDescriptor));
    write(RDH, 0);
    write(RDT, RX_COUNT - 1);
    write(TDBAL, (uint32_t)tx);
    write(TDBAH, (uint32_t)(tx >> 32));
    write(TDLEN, TX_COUNT * sizeof(TxDescriptor));
    write(TDH, 0);
    write(TDT, 0);
    write(TIPG, TIPG_DEFAULT);
    write(TCTL, TCTL_EN | TCTL_PSP | TCTL_CT | TCTL_COLD);
    write(RCTL, RCTL_EN | RCTL_BAM | RCTL_SECRC);

    read(ICR);
    write(IMS, INT_RXT0 | INT_RXDMT0 | INT_RXO | INT_LSC);
    if (!msi) {
        Pic::unmask(irq);
    }
    return 0;
}

void
E1000::stop() {
    if (!msi) {
        Pic::mask(irq);
    }
    write(IMC, 0xFFFFFFFFu);
    write(RCTL, 0);
    write(TCTL, TCTL_PSP);
}

bool
E1000::linkUp() const { return read(STATUS) & STATUS_LU; }

unsigned
E1000::linkMbps() const {
    uint32_t status = read(STATUS);
    if (!(status & STATUS_LU)) {
        return 0;
    }
    switch ((status & STATUS_SPEED_MASK) >> 6) {
        case 0: return 10;
        case 1: return 100;
        default: return 1000;
    }
}

int
E1000::send(PacketBuffer *frame) {
    size_t length = frame->length();
    if (length > BUFFER_SIZE) {
        PacketBuffer::release(frame);
        return -Err::INVAL;
    }
    LockGuard guard(txLock);
    volatile TxDescriptor &d = txRing[txNext];
    if (!(d.status & TX_DD)) {
        PacketBuffer::release(frame);
        return -Err::BUSY;
    }
    Memory::copy(txBuffers + txNext * BUFFER_SIZE, frame->data(), length);
    PacketBuffer::release(frame);
    d.length = (uint16_t)length;
    d.cmd = TX_EOP | TX_IFCS | TX_RS;
    d.status = 0;
    txNext = (txNext + 1) % TX_COUNT;
    asm volatile("" : : : "memory");
    write(TDT, txNext);
    return 0;
}

void
E1000::drainReceiveRing() {
    for (;;) {
        volatile RxDescriptor &d = rxRing[rxNext];
        if (!(d.status & RX_DD)) {
            break;
        }
        uint16_t length = d.length;
        bool good = (d.status & RX_EOP) && d.errors == 0 && length >= Ethernet::HEADER_SIZE;
        if (good) {
            PacketBuffer *packet = PacketBuffer::allocate();
            uint8_t *room = packet ? packet->put(length) : nullptr;
            if (room) {
                Memory::copy(room, rxBuffers + rxNext * BUFFER_SIZE, length);
                receive(packet);
            } else {
                PacketBuffer::release(packet);
                countRxDropped();
            }
        } else {
            countRxError();
        }
        d.status = 0;
        write(RDT, rxNext);
        rxNext = (rxNext + 1) % RX_COUNT;
    }
}

void
E1000::handleInterrupt() {
    uint32_t causes = read(ICR);
    if (causes & (INT_RXT0 | INT_RXDMT0 | INT_RXO)) {
        drainReceiveRing();
    }
}

TrapFrame *
E1000::onIrq(TrapFrame *frame) {
    if (instance) {
        instance->handleInterrupt();
        Net::processPending();
    }
    if (instance && instance->msi) {
        LocalApic::eoi();
    }
    return frame;
}
