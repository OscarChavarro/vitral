#include "drivers/net/tg3/Tg3.h"
#include "fs/Err.h"
#include "kernel/Interrupts.h"
#include "kernel/KernelHeap.h"
#include "kernel/KernelLog.h"
#include "kernel/LocalApic.h"
#include "kernel/Paging.h"
#include "lib/vitralKernel/LockGuard.h"
#include "lib/vitralKernel/Memory.h"
#include "net/Ethernet.h"
#include "net/Net.h"
#include "net/NetDevices.h"

Tg3 *Tg3::instance = nullptr;

Tg3::Tg3(volatile uint8_t *registers)
    : NetDevice("eth0", MacAddress()), mmio(registers), rxRing(nullptr), rxReturnRing(nullptr),
      txRing(nullptr), status(nullptr), rxBuffers(nullptr), txBuffers(nullptr), rxReturnNext(0), rxProducer(0),
      txProducer(0), txConsumer(0) {}

bool
Tg3::init() {
    Pci::Location at;
    if (!Pci::find(0x14e4, 0x1684, at)) {
        return false;
    }
    uint64_t address;
    uint64_t size;
    if (!Pci::memoryBar(at, 0, address, size) || address + size > Paging::mappedBytes()) {
        kprintf("Network: tg3 BCM5764M has an unusable register BAR\n");
        return false;
    }
    if (!LocalApic::supported()) {
        kprintf("Network: tg3 BCM5764M requires MSI because IRQ %u is outside the PIC\n",
                (uint32_t)Pci::read32(at, Pci::INTERRUPT_LINE));
        return false;
    }
    uint16_t command = Pci::read16(at, Pci::COMMAND);
    Pci::write16(at, Pci::COMMAND, (uint16_t)((command | Pci::COMMAND_MEMORY | Pci::COMMAND_BUS_MASTER) &
                                               ~Pci::COMMAND_INTERRUPT_DISABLE));
    Tg3 *card = new Tg3((volatile uint8_t *)address);
    MacAddress mac;
    if (!card->reset() || !card->readMac(mac) || !card->allocateRings()) {
        kprintf("Network: tg3 BCM5764M initialisation failed\n");
        delete card;
        return false;
    }
    LocalApic::enable();
    if (!Pci::enableMsi(at, MSI_VECTOR, LocalApic::id()) || !Interrupts::registerHandler(MSI_VECTOR, onIrq)) {
        kprintf("Network: tg3 BCM5764M cannot enable MSI\n");
        delete card;
        return false;
    }
    card->setMac(mac);
    instance = card;
    if (!NetDevices::add(card)) {
        Interrupts::unregisterHandler(MSI_VECTOR);
        instance = nullptr;
        delete card;
        return false;
    }
    char text[MacAddress::TEXT_SIZE];
    mac.format(text);
    kprintf("Network: eth0 Broadcom BCM5764M tg3 (PCI %u:%u.%u) MAC %s, MSI vector %u\n", (uint32_t)at.bus,
            (uint32_t)at.device, (uint32_t)at.function, text, (uint32_t)MSI_VECTOR);
    return true;
}

bool
Tg3::readMac(MacAddress &out) const {
    uint32_t high = read(MAC_ADDR_0_HIGH);
    uint32_t low = read(MAC_ADDR_0_LOW);
    uint8_t bytes[6];
    bytes[0] = (uint8_t)(high >> 8);
    bytes[1] = (uint8_t)high;
    bytes[2] = (uint8_t)(low >> 24);
    bytes[3] = (uint8_t)(low >> 16);
    bytes[4] = (uint8_t)(low >> 8);
    bytes[5] = (uint8_t)low;
    out = MacAddress::from(bytes);
    return !out.isZero() && !out.isMulticast();
}

bool
Tg3::allocateRings() {
    rxRing = (volatile Descriptor *)KernelHeap::tryAllocateAligned(RX_COUNT * sizeof(Descriptor), 16);
    rxReturnRing = (volatile Descriptor *)KernelHeap::tryAllocateAligned(RX_RETURN_COUNT * sizeof(Descriptor), 16);
    txRing = (volatile Descriptor *)KernelHeap::tryAllocateAligned(TX_COUNT * sizeof(Descriptor), 16);
    status = (volatile Status *)KernelHeap::tryAllocateAligned(sizeof(Status), 64);
    rxBuffers = (uint8_t *)KernelHeap::tryAllocateAligned(RX_COUNT * BUFFER_SIZE, 16);
    txBuffers = (uint8_t *)KernelHeap::tryAllocateAligned(TX_COUNT * BUFFER_SIZE, 16);
    if (!rxRing || !rxReturnRing || !txRing || !status || !rxBuffers || !txBuffers) {
        return false;
    }
    Memory::set((void *)rxRing, 0, RX_COUNT * sizeof(Descriptor));
    Memory::set((void *)rxReturnRing, 0, RX_RETURN_COUNT * sizeof(Descriptor));
    Memory::set((void *)txRing, 0, TX_COUNT * sizeof(Descriptor));
    Memory::set((void *)status, 0, sizeof(Status));
    for (unsigned i = 0; i < RX_COUNT; i++) {
        uint64_t address = (uint64_t)(rxBuffers + i * BUFFER_SIZE);
        rxRing[i].addressHigh = (uint32_t)(address >> 32);
        rxRing[i].addressLow = (uint32_t)address;
        rxRing[i].first = i << 16 | BUFFER_SIZE;
        rxRing[i].second = RXD_OPAQUE_RING_STD | i;
    }
    return true;
}

void
Tg3::pause(unsigned cycles) {
    for (unsigned i = 0; i < cycles; i++) {
        asm volatile("pause");
    }
}

bool
Tg3::reset() {
    write(MAILBOX_INTERRUPT_0, 1);
    static const uint32_t ENGINES[] = {MAC_RX_MODE, MAC_TX_MODE, SNDDATAI_MODE, SNDBDS_MODE, SNDBDI_MODE,
                                       SNDBDC_MODE, RCVLPC_MODE, RCVDBDI_MODE, RCVDCC_MODE, RCVBDI_MODE,
                                       RCVCC_MODE, HOSTCC_MODE, BUFMGR_MODE, RDMAC_MODE, WDMAC_MODE};
    for (uint32_t engine : ENGINES) {
        write(engine, 1);
    }
    write(MAC_MODE, 1);
    write(GRC_MISC_CFG, read(GRC_MISC_CFG) | 1);
    pause(100000);
    write(GRC_MISC_CFG, read(GRC_MISC_CFG) & ~1u);
    pause(100000);
    write(GRC_MODE, GRC_MODE_HOST_SENDBDS);
    write(MAC_MI_MODE, 0x000c0000);
    return initializePhy();
}

bool
Tg3::readPhy(unsigned reg, uint16_t &value) {
    write(MAC_MI_COM, 0x08000000u | 0x20000000u | (1u << 21) | (reg << 16));
    for (unsigned i = 0; i < 5000; i++) {
        uint32_t command = read(MAC_MI_COM);
        if (!(command & 0x20000000u)) {
            value = (uint16_t)command;
            return !(command & 0x10000000u);
        }
        pause(100);
    }
    return false;
}

bool
Tg3::writePhy(unsigned reg, uint16_t value) {
    write(MAC_MI_COM, 0x04000000u | 0x20000000u | (1u << 21) | (reg << 16) | value);
    for (unsigned i = 0; i < 5000; i++) {
        if (!(read(MAC_MI_COM) & 0x20000000u)) {
            return true;
        }
        pause(100);
    }
    return false;
}

bool
Tg3::initializePhy() {
    uint16_t id;
    if (!readPhy(2, id) || id == 0 || id == 0xFFFF) {
        return false;
    }
    if (!writePhy(4, 0x01E1) || !writePhy(9, 0x0200) || !writePhy(0, 0x1200)) {
        return false;
    }
    return true;
}

void
Tg3::writeMemory(uint32_t address, uint32_t value) {
    write(GRC_MEM_WIN_BASE, address);
    write(GRC_MEM_WIN_DATA, value);
    write(GRC_MEM_WIN_BASE, 0);
}

void
Tg3::writeBdInfo(uint32_t address, uint64_t host, uint32_t maxLength, uint32_t nic) {
    write(address, (uint32_t)(host >> 32));
    write(address + 4, (uint32_t)host);
    write(address + 8, maxLength << 16);
    write(address + 12, nic);
}

int
Tg3::start() {
    rxReturnNext = 0;
    rxProducer = RX_POSTED;
    txProducer = 0;
    txConsumer = 0;
    Memory::set((void *)rxReturnRing, 0, RX_RETURN_COUNT * sizeof(Descriptor));
    Memory::set((void *)txRing, 0, TX_COUNT * sizeof(Descriptor));
    Memory::set((void *)status, 0, sizeof(Status));
    write(MAILBOX_INTERRUPT_0, 1);
    write(GRC_MODE, read(GRC_MODE) | GRC_MODE_HOST_SENDBDS);
    write(RCVDBDI_STD_BD, (uint32_t)((uint64_t)rxRing >> 32));
    write(RCVDBDI_STD_BD + 4, (uint32_t)(uint64_t)rxRing);
    write(RCVDBDI_STD_BD + 8, BUFFER_SIZE << 16);
    write(RCVDBDI_STD_BD + 12, NIC_SRAM_RX_BUFFER_DESC);
    writeMemory(NIC_SRAM_SEND_RCB, (uint32_t)((uint64_t)txRing >> 32));
    writeMemory(NIC_SRAM_SEND_RCB + 4, (uint32_t)(uint64_t)txRing);
    writeMemory(NIC_SRAM_SEND_RCB + 8, TX_COUNT << 16);
    writeMemory(NIC_SRAM_SEND_RCB + 12, NIC_SRAM_TX_BUFFER_DESC);
    writeMemory(NIC_SRAM_RCV_RET_RCB, (uint32_t)((uint64_t)rxReturnRing >> 32));
    writeMemory(NIC_SRAM_RCV_RET_RCB + 4, (uint32_t)(uint64_t)rxReturnRing);
    writeMemory(NIC_SRAM_RCV_RET_RCB + 8, RX_RETURN_COUNT << 16);
    write(HOSTCC_STATUS_BLK_HOST_ADDR, (uint32_t)((uint64_t)status >> 32));
    write(HOSTCC_STATUS_BLK_HOST_ADDR + 4, (uint32_t)(uint64_t)status);
    write(HOSTCC_RXCOL_TICKS, 0);
    write(HOSTCC_TXCOL_TICKS, 0);
    write(HOSTCC_RXMAX_FRAMES, 1);
    write(HOSTCC_TXMAX_FRAMES, 1);
    write(HOSTCC_MODE, MODE_ENABLE | 0x80);
    write(RCVLPC_MODE, MODE_ENABLE);
    write(RCVDCC_MODE, MODE_ENABLE | MODE_ATTN_ENABLE);
    write(RCVBDI_MODE, MODE_ENABLE | MODE_ATTN_ENABLE);
    write(RCVDBDI_MODE, MODE_ENABLE);
    write(RCVCC_MODE, MODE_ENABLE | MODE_ATTN_ENABLE);
    write(SNDDATAI_MODE, MODE_ENABLE);
    write(SNDBDI_MODE, MODE_ENABLE | MODE_ATTN_ENABLE);
    write(SNDBDS_MODE, MODE_ENABLE | MODE_ATTN_ENABLE);
    write(SNDBDC_MODE, MODE_ENABLE | MODE_ATTN_ENABLE);
    write(RDMAC_MODE, MODE_ENABLE);
    write(WDMAC_MODE, MODE_ENABLE);
    write(BUFMGR_MODE, MODE_ENABLE | MODE_ATTN_ENABLE);
    write(MAC_TX_MODE, MODE_ENABLE);
    write(MAC_RX_MODE, MODE_ENABLE);
    write(MAC_MODE, read(MAC_MODE) | MAC_MODE_RXSTAT_ENABLE | MAC_MODE_TXSTAT_ENABLE | MAC_MODE_TDE_ENABLE |
                         MAC_MODE_RDE_ENABLE);
    asm volatile("" : : : "memory");
    write(MAILBOX_RCV_STD_PROD_IDX, rxProducer);
    write(MAILBOX_RCVRET_CON_IDX_0, 0);
    write(MAILBOX_INTERRUPT_0, 0);
    return 0;
}

void
Tg3::stop() {
    write(MAILBOX_INTERRUPT_0, 1);
    write(MAC_RX_MODE, 0);
    write(MAC_TX_MODE, 0);
    write(RCVDBDI_MODE, 0);
    write(SNDBDI_MODE, 0);
    write(HOSTCC_MODE, 0);
}

bool
Tg3::hasLink() const { return read(MAC_TX_STATUS) & MAC_TX_STATUS_LINK_UP; }

int
Tg3::send(PacketBuffer *frame) {
    size_t length = frame->length();
    if (length > BUFFER_SIZE) {
        PacketBuffer::release(frame);
        return -Err::INVAL;
    }
    LockGuard guard(txLock);
    retireTransmit();
    unsigned next = (txProducer + 1) % TX_COUNT;
    if (next == txConsumer) {
        PacketBuffer::release(frame);
        return -Err::BUSY;
    }
    Memory::copy(txBuffers + txProducer * BUFFER_SIZE, frame->data(), length);
    PacketBuffer::release(frame);
    uint64_t address = (uint64_t)(txBuffers + txProducer * BUFFER_SIZE);
    txRing[txProducer].addressHigh = (uint32_t)(address >> 32);
    txRing[txProducer].addressLow = (uint32_t)address;
    txRing[txProducer].first = (uint32_t)length << 16 | TXD_FLAG_END;
    txRing[txProducer].second = 0;
    txProducer = next;
    asm volatile("" : : : "memory");
    write(MAILBOX_SNDHOST_PROD_IDX_0, txProducer);
    return 0;
}

void
Tg3::retireTransmit() {
    unsigned complete = status->indices[0].txConsumer % TX_COUNT;
    txConsumer = complete;
}

void
Tg3::drainReceiveRing() {
    unsigned produced = status->indices[0].rxProducer % RX_RETURN_COUNT;
    while (rxReturnNext != produced) {
        volatile Descriptor &returned = rxReturnRing[rxReturnNext];
        unsigned index = returned.second & 0xFFFF;
        unsigned length = returned.first & 0xFFFF;
        bool good = index < RX_COUNT && length >= Ethernet::HEADER_SIZE && !(returned.second & RXD_FLAG_ERROR);
        if (good) {
            PacketBuffer *packet = PacketBuffer::allocate();
            uint8_t *room = packet ? packet->put(length - 4) : nullptr;
            if (room) {
                Memory::copy(room, rxBuffers + index * BUFFER_SIZE, length - 4);
                receive(packet);
            } else {
                PacketBuffer::release(packet);
                countRxDropped();
            }
        } else {
            countRxError();
        }
        rxRing[index].first = index << 16 | BUFFER_SIZE;
        rxRing[index].second = RXD_OPAQUE_RING_STD | index;
        rxProducer = (rxProducer + 1) % RX_COUNT;
        rxReturnNext = (rxReturnNext + 1) % RX_RETURN_COUNT;
    }
    asm volatile("" : : : "memory");
    write(MAILBOX_RCV_STD_PROD_IDX, rxProducer);
    write(MAILBOX_RCVRET_CON_IDX_0, rxReturnNext);
}

void
Tg3::handleInterrupt() {
    write(MAILBOX_INTERRUPT_0, 1);
    drainReceiveRing();
    retireTransmit();
    write(GRC_LOCAL_CTRL, read(GRC_LOCAL_CTRL) | 2);
    write(MAILBOX_INTERRUPT_0, 0);
}

TrapFrame *
Tg3::onIrq(TrapFrame *frame) {
    if (instance) {
        instance->handleInterrupt();
        Net::processPending();
    }
    LocalApic::eoi();
    return frame;
}
