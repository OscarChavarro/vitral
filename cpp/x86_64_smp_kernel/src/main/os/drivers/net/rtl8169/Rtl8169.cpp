#include "drivers/net/rtl8169/Rtl8169.h"
#include "fs/Err.h"
#include "kernel/Interrupts.h"
#include "kernel/KernelHeap.h"
#include "kernel/KernelLog.h"
#include "kernel/Paging.h"
#include "kernel/Pic.h"
#include "lib/vitralKernel/LockGuard.h"
#include "lib/vitralKernel/Memory.h"
#include "net/Ethernet.h"
#include "net/Net.h"
#include "net/NetDevices.h"

Rtl8169 *Rtl8169::instance = nullptr;

Rtl8169::Rtl8169(volatile uint8_t *registers, uint8_t irqLine)
    : NetDevice("eth0", MacAddress()), mmio(registers), irq(irqLine), rxRing(nullptr), txRing(nullptr),
      rxBuffers(nullptr), txBuffers(nullptr), rxNext(0), txNext(0) {}

bool
Rtl8169::init() {
    Pci::Location at;
    if (!Pci::find(0x10ec, 0x8169, at)) {
        return false;
    }
    uint64_t address;
    uint64_t size;
    if (!Pci::memoryBar(at, 1, address, size) || size < 0x100 || address + size > Paging::mappedBytes()) {
        kprintf("Network: RTL8169 found but its register BAR is unusable\n");
        return false;
    }
    uint8_t line = Pci::read8(at, Pci::INTERRUPT_LINE);
    if (line >= 16) {
        kprintf("Network: RTL8169 has no PIC interrupt line (%u)\n", (uint32_t)line);
        return false;
    }
    uint16_t command = Pci::read16(at, Pci::COMMAND);
    command |= Pci::COMMAND_MEMORY;
    command |= Pci::COMMAND_BUS_MASTER;
    command &= ~Pci::COMMAND_INTERRUPT_DISABLE;
    Pci::write16(at, Pci::COMMAND, command);

    Rtl8169 *card = new Rtl8169((volatile uint8_t *)address, line);
    MacAddress mac = MacAddress::from((const uint8_t *)card->mmio + MAC0);
    if (!card->reset() || mac.isZero() || mac.isMulticast() || !card->allocateRings()) {
        kprintf("Network: RTL8169 initialisation failed\n");
        delete card;
        return false;
    }
    if (!Interrupts::registerHandler(Interrupts::IRQ_BASE + line, onIrq)) {
        kprintf("Network: RTL8169 interrupt vector already taken\n");
        delete card;
        return false;
    }
    card->setMac(mac);
    instance = card;
    if (!NetDevices::add(card)) {
        Interrupts::unregisterHandler(Interrupts::IRQ_BASE + line);
        instance = nullptr;
        delete card;
        return false;
    }
    char text[MacAddress::TEXT_SIZE];
    mac.format(text);
    kprintf("Network: eth0 Realtek RTL8169 (PCI %u:%u.%u) MAC %s, IRQ %u, link %s\n", (uint32_t)at.bus,
            (uint32_t)at.device, (uint32_t)at.function, text, (uint32_t)line, card->hasLink() ? "up" : "down");
    return true;
}

bool
Rtl8169::reset() {
    write16(INTERRUPT_MASK, 0);
    write8(CONFIG_9346, CONFIG_9346_UNLOCK);
    write8(CHIP_COMMAND, COMMAND_RESET);
    for (unsigned spin = 0; spin < 1000000; spin++) {
        if (!(read8(CHIP_COMMAND) & COMMAND_RESET)) {
            write8(CONFIG_9346, CONFIG_9346_LOCK);
            return true;
        }
        asm volatile("pause");
    }
    write8(CONFIG_9346, CONFIG_9346_LOCK);
    return false;
}

bool
Rtl8169::allocateRings() {
    rxRing = (volatile Descriptor *)KernelHeap::tryAllocateAligned(RX_COUNT * sizeof(Descriptor), 256);
    txRing = (volatile Descriptor *)KernelHeap::tryAllocateAligned(TX_COUNT * sizeof(Descriptor), 256);
    rxBuffers = (uint8_t *)KernelHeap::tryAllocateAligned(RX_COUNT * BUFFER_SIZE, 16);
    txBuffers = (uint8_t *)KernelHeap::tryAllocateAligned(TX_COUNT * BUFFER_SIZE, 16);
    if (!rxRing || !txRing || !rxBuffers || !txBuffers) {
        return false;
    }
    for (unsigned i = 0; i < RX_COUNT; i++) {
        rxRing[i].options1 = DESC_OWN | (i == RX_COUNT - 1 ? DESC_END : 0);
        rxRing[i].options2 = 0;
        rxRing[i].address = (uint64_t)(rxBuffers + i * BUFFER_SIZE);
    }
    for (unsigned i = 0; i < TX_COUNT; i++) {
        txRing[i].options1 = i == TX_COUNT - 1 ? DESC_END : 0;
        txRing[i].options2 = 0;
        txRing[i].address = (uint64_t)(txBuffers + i * BUFFER_SIZE);
    }
    return true;
}

int
Rtl8169::start() {
    rxNext = 0;
    txNext = 0;
    for (unsigned i = 0; i < RX_COUNT; i++) {
        rxRing[i].options1 = DESC_OWN | (i == RX_COUNT - 1 ? DESC_END : 0);
    }
    for (unsigned i = 0; i < TX_COUNT; i++) {
        txRing[i].options1 = i == TX_COUNT - 1 ? DESC_END : 0;
    }
    uint64_t rxAddress = (uint64_t)rxRing;
    uint64_t txAddress = (uint64_t)txRing;
    write32(RX_DESC_LOW, (uint32_t)rxAddress);
    write32(RX_DESC_HIGH, (uint32_t)(rxAddress >> 32));
    write32(TX_DESC_LOW, (uint32_t)txAddress);
    write32(TX_DESC_HIGH, (uint32_t)(txAddress >> 32));
    write32(TX_CONFIG, TX_DMA_BURST_1024 | TX_INTERFRAME_GAP);
    write32(RX_CONFIG, RX_ACCEPT_PHYSICAL | RX_ACCEPT_MULTICAST | RX_ACCEPT_BROADCAST | RX_DMA_BURST_UNLIMITED |
                           RX_BUFFER_8K);
    write16(INTERRUPT_STATUS, 0xffff);
    write8(CHIP_COMMAND, COMMAND_RX_ENABLE | COMMAND_TX_ENABLE);
    write16(INTERRUPT_MASK, INTERRUPT_RX_OK | INTERRUPT_RX_ERROR | INTERRUPT_RX_OVERFLOW |
                              INTERRUPT_RX_FIFO_OVERFLOW | INTERRUPT_TX_ERROR);
    Pic::unmask(irq);
    return 0;
}

void
Rtl8169::stop() {
    Pic::mask(irq);
    write16(INTERRUPT_MASK, 0);
    write8(CHIP_COMMAND, 0);
}

bool
Rtl8169::hasLink() const { return read8(PHY_STATUS) & PHY_LINK_UP; }

int
Rtl8169::send(PacketBuffer *frame) {
    size_t length = frame->length();
    if (length > BUFFER_SIZE) {
        PacketBuffer::release(frame);
        return -Err::INVAL;
    }
    LockGuard guard(txLock);
    volatile Descriptor &descriptor = txRing[txNext];
    if (descriptor.options1 & DESC_OWN) {
        PacketBuffer::release(frame);
        return -Err::BUSY;
    }
    Memory::copy(txBuffers + txNext * BUFFER_SIZE, frame->data(), length);
    PacketBuffer::release(frame);
    uint32_t flags = DESC_OWN | DESC_FIRST | DESC_LAST | (uint32_t)length;
    if (txNext == TX_COUNT - 1) {
        flags |= DESC_END;
    }
    descriptor.options1 = flags;
    txNext = (txNext + 1) % TX_COUNT;
    asm volatile("" : : : "memory");
    write8(TX_POLL, TX_POLL_NORMAL);
    return 0;
}

void
Rtl8169::drainReceiveRing() {
    for (;;) {
        volatile Descriptor &descriptor = rxRing[rxNext];
        uint32_t options = descriptor.options1;
        if (options & DESC_OWN) {
            break;
        }
        size_t length = options & DESC_LENGTH;
        bool good = !(options & DESC_RX_ERROR) && (options & DESC_FIRST) && (options & DESC_LAST) &&
                    length >= Ethernet::HEADER_SIZE + ETHERNET_FCS && length <= BUFFER_SIZE;
        if (good) {
            length -= ETHERNET_FCS;
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
        descriptor.options1 = DESC_OWN | (rxNext == RX_COUNT - 1 ? DESC_END : 0);
        rxNext = (rxNext + 1) % RX_COUNT;
    }
}

void
Rtl8169::handleInterrupt() {
    uint16_t causes = read16(INTERRUPT_STATUS);
    if (!causes) {
        return;
    }
    write16(INTERRUPT_STATUS, causes);
    if (causes & (INTERRUPT_RX_OK | INTERRUPT_RX_ERROR | INTERRUPT_RX_OVERFLOW | INTERRUPT_RX_FIFO_OVERFLOW)) {
        drainReceiveRing();
    }
    if (causes & INTERRUPT_TX_ERROR) {
        countTxError();
    }
}

TrapFrame *
Rtl8169::onIrq(TrapFrame *frame) {
    if (instance) {
        instance->handleInterrupt();
        Net::processPending();
    }
    return frame;
}
