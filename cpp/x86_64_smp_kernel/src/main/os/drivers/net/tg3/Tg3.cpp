#include "drivers/net/tg3/Tg3.h"
#include "fs/Err.h"
#include "kernel/Clock.h"
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

Tg3::Tg3(volatile uint8_t *registers, const Pci::Location &where)
    : NetDevice("eth0", MacAddress()), mmio(registers), at(where), rxRing(nullptr), rxReturnRing(nullptr),
      txRing(nullptr), status(nullptr), rxBuffers(nullptr), txBuffers(nullptr), rxReturnNext(0), rxProducer(0),
      txProducer(0), txConsumer(0), tracedRx(0), tracedTx(0), interrupts(0), errorsTraced(0), linkUp(false),
      linkCheckedMs(0), chipRevision(0) {}

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
    kprintf("Network: tg3 BCM5764M at PCI %u:%u.%u, registers at 0x%x%08x (%u KB)\n", (uint32_t)at.bus,
            (uint32_t)at.device, (uint32_t)at.function, (uint32_t)(address >> 32), (uint32_t)address,
            (uint32_t)(size >> 10));
    uint16_t command = Pci::read16(at, Pci::COMMAND);
    Pci::write16(at, Pci::COMMAND, (uint16_t)((command | Pci::COMMAND_MEMORY | Pci::COMMAND_BUS_MASTER) &
                                               ~Pci::COMMAND_INTERRUPT_DISABLE));
    Tg3 *card = new Tg3((volatile uint8_t *)address, at);
    MacAddress mac;
    if (!card->reset()) {
        delete card;
        return false;
    }
    if (!card->readMac(mac)) {
        kprintf("Network: tg3 BCM5764M has no MAC address (SRAM 0x%08x %08x, registers 0x%08x %08x)\n",
                card->readMemory(NIC_SRAM_MAC_ADDR_HIGH), card->readMemory(NIC_SRAM_MAC_ADDR_LOW),
                card->read(MAC_ADDR_0_HIGH), card->read(MAC_ADDR_0_LOW));
        delete card;
        return false;
    }
    if (!card->allocateRings()) {
        kprintf("Network: tg3 BCM5764M initialisation failed: no memory for its rings\n");
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

// As Linux: the address the bootcode left in its SRAM mailbox (marked "HK"), else the MAC's own registers.
bool
Tg3::readMac(MacAddress &out) const {
    uint32_t high = readMemory(NIC_SRAM_MAC_ADDR_HIGH);
    uint32_t low = readMemory(NIC_SRAM_MAC_ADDR_LOW);
    if ((high >> 16) != 0x484b) {
        high = read(MAC_ADDR_0_HIGH);
        low = read(MAC_ADDR_0_LOW);
    }
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
    rxRing = (volatile RxDescriptor *)KernelHeap::tryAllocateAligned(RX_COUNT * sizeof(RxDescriptor), 64);
    rxReturnRing =
        (volatile RxDescriptor *)KernelHeap::tryAllocateAligned(RX_RETURN_COUNT * sizeof(RxDescriptor), 64);
    txRing = (volatile TxDescriptor *)KernelHeap::tryAllocateAligned(TX_COUNT * sizeof(TxDescriptor), 64);
    status = (volatile Status *)KernelHeap::tryAllocateAligned(sizeof(Status), 64);
    rxBuffers = (uint8_t *)KernelHeap::tryAllocateAligned(RX_COUNT * BUFFER_SIZE, 64);
    txBuffers = (uint8_t *)KernelHeap::tryAllocateAligned(TX_COUNT * BUFFER_SIZE, 64);
    if (!rxRing || !rxReturnRing || !txRing || !status || !rxBuffers || !txBuffers) {
        return false;
    }
    Memory::set((void *)rxRing, 0, RX_COUNT * sizeof(RxDescriptor));
    Memory::set((void *)rxReturnRing, 0, RX_RETURN_COUNT * sizeof(RxDescriptor));
    Memory::set((void *)txRing, 0, TX_COUNT * sizeof(TxDescriptor));
    Memory::set((void *)status, 0, sizeof(Status));
    for (unsigned i = 0; i < RX_COUNT; i++) {
        uint64_t address = (uint64_t)(rxBuffers + i * BUFFER_SIZE);
        rxRing[i].addressHigh = (uint32_t)(address >> 32);
        rxRing[i].addressLow = (uint32_t)address;
        rxRing[i].indexLength = RX_DMA_SIZE;
        rxRing[i].typeFlags = RXD_FLAG_END;
        rxRing[i].opaque = RXD_OPAQUE_RING_STD | i;
    }
    return true;
}

void
Tg3::pause(unsigned cycles) {
    for (unsigned i = 0; i < cycles; i++) {
        asm volatile("pause");
    }
}

void
Tg3::delayUs(uint32_t microseconds) {
    uint64_t end = Clock::nanoseconds() + (uint64_t)microseconds * 1000;
    while (Clock::nanoseconds() < end) {
        asm volatile("pause");
    }
}

void
Tg3::delayMs(uint32_t milliseconds) {
    uint64_t end = Clock::uptimeMs() + milliseconds + 1;
    while (Clock::uptimeMs() < end) {
        asm volatile("pause");
    }
}

// Each round reads a register (about a microsecond on PCI Express) and pauses.
bool
Tg3::waitBit(uint32_t reg, uint32_t bit, bool set, unsigned microseconds) const {
    for (unsigned i = 0; i < microseconds; i++) {
        if (((read(reg) & bit) != 0) == set) {
            return true;
        }
        pause(20);
    }
    return false;
}

// The chip reset of Linux's tg3_chip_reset(), reduced to what the 5784 family (BCM5764M, PCI Express) needs: the
// core clock reset clears the PCI state of the function, which is then written back, and the bootcode runs again
// (it loads the MAC address and sets the PHY up) before the driver may go on.
uint8_t
Tg3::pcieCapability() const {
    uint8_t capability = Pci::read8(at, 0x34) & ~3u;
    for (unsigned count = 0; capability >= 0x40 && count < 48; count++) {
        if (Pci::read8(at, capability) == PCI_CAPABILITY_PCIE) {
            return capability;
        }
        capability = Pci::read8(at, (uint8_t)(capability + 1)) & ~3u;
    }
    return 0;
}

// tg3_detect_asic_rev(): the revision is in MISC_HOST_CTRL, or for the newer chips (the 5784 family among them) in
// the product id register.
bool
Tg3::reset() {
    chipRevision = Pci::read32(at, PCI_MISC_HOST_CTRL) >> 16;
    if ((chipRevision >> 12) == ASIC_REV_USE_PROD_ID_REG) {
        chipRevision = Pci::read32(at, PCI_PRODID_ASICREV);
    }
    uint16_t command = Pci::read16(at, Pci::COMMAND);
    uint8_t pcie = pcieCapability();
    uint16_t deviceControl = pcie ? Pci::read16(at, (uint8_t)(pcie + PCIE_DEVICE_CONTROL)) : 0;
    uint32_t hostControl = MISC_HOST_CTRL_MASK_PCI_INT | MISC_HOST_CTRL_WORD_SWAP | MISC_HOST_CTRL_PCISTATE_RW |
                           MISC_HOST_CTRL_INDIR_ACCESS;
    kprintf("Network: tg3 before reset: MISC_HOST_CTRL 0x%08x, GRC_MODE 0x%08x, MAC_MODE 0x%08x, firmware "
            "mailbox 0x%08x\n", Pci::read32(at, PCI_MISC_HOST_CTRL), read(GRC_MODE), read(MAC_MODE),
            readMemory(NIC_SRAM_FIRMWARE_MBOX));
    Pci::write32(at, PCI_MISC_HOST_CTRL, hostControl);
    write(MAILBOX_INTERRUPT_0, 1);
    writeMemory(NIC_SRAM_FIRMWARE_MBOX, FIRMWARE_MBOX_MAGIC);
    uint32_t nicConfig = readMemory(NIC_SRAM_DATA_CFG);
    kprintf("Network: tg3 chip revision 0x%x, NIC configuration 0x%08x (ASF %s, APE %s)\n", chipRevision, nicConfig,
            nicConfig & NIC_SRAM_DATA_CFG_ASF_ENABLE ? "on" : "off",
            nicConfig & NIC_SRAM_DATA_CFG_APE_ENABLE ? "on" : "off");
    // tg3_chip_reset() of a PCI Express 5755 or later: the fast boot program counter cleared, then the core clock
    // reset with bit 29 (written first on its own); the 5784 has a CPMU, so the PHY power is not kept
    write(GRC_FASTBOOT_PC, 0);
    write(GRC_MISC_CFG, GRC_MISC_CFG_PCIE);
    write(GRC_MISC_CFG, GRC_MISC_CFG_PCIE | GRC_MISC_CFG_CORECLK_RESET);
    delayMs(RESET_WAIT_MS);
    Pci::write16(at, Pci::COMMAND, command);
    uint16_t resetControl = 0;
    if (pcie) {
        // As pci_restore_state() and tg3_chip_reset(): the device control of before, without relaxed ordering and
        // no snoop, which the reset turned back on
        resetControl = Pci::read16(at, (uint8_t)(pcie + PCIE_DEVICE_CONTROL));
        Pci::write16(at, (uint8_t)(pcie + PCIE_DEVICE_CONTROL),
                     (uint16_t)(deviceControl & ~(PCIE_RELAXED_ORDERING | PCIE_NO_SNOOP)));
        // The errors the reset may have recorded (correctable, non-fatal, fatal, unsupported request)
        Pci::write16(at, (uint8_t)(pcie + PCIE_DEVICE_STATUS), PCIE_DEVICE_STATUS_ERRORS);
    }
    Pci::write32(at, PCI_MISC_HOST_CTRL, hostControl);
    Pci::write32(at, PCI_DMA_RW_CTRL, DMA_RW_CTRL_PCIE);
    write(MEMARB_MODE, read(MEMARB_MODE) | MODE_ENABLE);
    uint32_t firmware = 0;
    uint64_t start = Clock::uptimeMs();
    uint32_t waited = 0;
    while (waited < FIRMWARE_WAIT_MS) {
        firmware = readMemory(NIC_SRAM_FIRMWARE_MBOX);
        if (firmware == ~FIRMWARE_MBOX_MAGIC) {
            break;
        }
        pause(1000);
        waited = (uint32_t)(Clock::uptimeMs() - start);
    }
    kprintf("Network: tg3 reset: PCI Express capability 0x%x, device control 0x%04x before, 0x%04x after the "
            "reset, now 0x%04x\n", (uint32_t)pcie, (uint32_t)deviceControl, (uint32_t)resetControl,
            pcie ? (uint32_t)Pci::read16(at, (uint8_t)(pcie + PCIE_DEVICE_CONTROL)) : 0);
    kprintf("Network: tg3 reset: PCI command 0x%x, GRC_MISC_CFG 0x%08x, bootcode %s after %u ms (mailbox "
            "0x%08x)\n", (uint32_t)Pci::read16(at, Pci::COMMAND), read(GRC_MISC_CFG),
            firmware == ~FIRMWARE_MBOX_MAGIC ? "done" : "silent", waited, firmware);
    write(GRC_MODE, GRC_MODE_VALUE);
    // As tg3_chip_reset(): the MAC port off until the link is known, the MDIO interface, then the PCI Express
    // transaction setting Linux gives every PCI Express chip before the 57765
    writeFlush(MAC_MODE, 0);
    pause(4000);
    write(MAC_MI_MODE, MAC_MI_MODE_VALUE);
    write(PCIE_TRANSACTION_CFG, read(PCIE_TRANSACTION_CFG) | PCIE_TRANSACTION_CFG_LINUX);
    delayMs(1);
    return initializePhy();
}

// As __tg3_readphy(): once BUSY clears, the data is read again 5 microseconds later (the read that shows BUSY clear
// can still carry the previous frame's data, often 0)
bool
Tg3::readPhy(unsigned reg, uint16_t &value) const {
    value = 0;
    writeFlush(MAC_MI_COM, 0x08000000u | 0x20000000u | (PHY_ADDRESS << 21) | (reg << 16));
    for (unsigned i = 0; i < PHY_BUSY_LOOPS; i++) {
        delayUs(10);
        if (!(read(MAC_MI_COM) & 0x20000000u)) {
            delayUs(5);
            uint32_t command = read(MAC_MI_COM);
            value = (uint16_t)command;
            return !(command & 0x10000000u);
        }
    }
    return false;
}

bool
Tg3::writePhy(unsigned reg, uint16_t value) const {
    writeFlush(MAC_MI_COM, 0x04000000u | 0x20000000u | (PHY_ADDRESS << 21) | (reg << 16) | value);
    for (unsigned i = 0; i < PHY_BUSY_LOOPS; i++) {
        delayUs(10);
        if (!(read(MAC_MI_COM) & 0x20000000u)) {
            delayUs(5);
            return true;
        }
    }
    return false;
}

// Advertises 10, 100 and 1000 Mb/s in both duplexes and restarts the autonegotiation.
bool
Tg3::initializePhy() {
    LockGuard guard(phyLock);
    uint16_t id = 0;
    uint16_t id2 = 0;
    uint16_t bmsr = 0;
    if (!readPhy(2, id) || !readPhy(3, id2) || id == 0 || id == 0xFFFF) {
        kprintf("Network: tg3: no PHY answers (id 0x%04x%04x, MI_COM 0x%08x, MI_MODE 0x%08x)\n", (uint32_t)id,
                (uint32_t)id2, read(MAC_MI_COM), read(MAC_MI_MODE));
        return false;
    }
    readPhy(MII_BMSR, bmsr);
    if (!writePhy(MII_ADVERTISE, 0x01E1) || !writePhy(MII_CTRL1000, 0x0300) || !writePhy(MII_BMCR, 0x1200)) {
        kprintf("Network: tg3: PHY 0x%04x%04x refuses its settings\n", (uint32_t)id, (uint32_t)id2);
        return false;
    }
    kprintf("Network: tg3: PHY 0x%04x%04x (status 0x%04x), autonegotiation restarted\n", (uint32_t)id,
            (uint32_t)id2, (uint32_t)bmsr);
    return true;
}

// As Linux's tg3_write_mem() and tg3_read_mem(): every write to the window is flushed by reading it back, since the
// window base (a register of the PCI configuration shadow) and the window data (NIC memory) are not reached in order.
void
Tg3::writeMemory(uint32_t address, uint32_t value) const {
    writeFlush(GRC_MEM_WIN_BASE, address);
    writeFlush(GRC_MEM_WIN_DATA, value);
    writeFlush(GRC_MEM_WIN_BASE, 0);
}

uint32_t
Tg3::readMemory(uint32_t address) const {
    writeFlush(GRC_MEM_WIN_BASE, address);
    uint32_t value = read(GRC_MEM_WIN_DATA);
    writeFlush(GRC_MEM_WIN_BASE, 0);
    return value;
}

// A ring control block in NIC memory: host address, then maximum length and flags, then NIC address.
void
Tg3::writeRcb(uint32_t address, uint64_t host, uint32_t maxLengthFlags, uint32_t nic) const {
    writeMemory(address, (uint32_t)(host >> 32));
    writeMemory(address + 4, (uint32_t)host);
    writeMemory(address + 8, maxLengthFlags);
    writeMemory(address + 12, nic);
}

void
Tg3::setMacAddress(const MacAddress &mac) {
    const uint8_t *b = mac.data();
    uint32_t high = (uint32_t)b[0] << 8 | b[1];
    uint32_t low = (uint32_t)b[2] << 24 | (uint32_t)b[3] << 16 | (uint32_t)b[4] << 8 | b[5];
    for (unsigned i = 0; i < 4; i++) {
        write(MAC_ADDR_0_HIGH + i * 8, high);
        write(MAC_ADDR_0_LOW + i * 8, low);
    }
    uint32_t seed = 0;
    for (unsigned i = 0; i < 6; i++) {
        seed += b[i];
    }
    write(MAC_TX_BACKOFF_SEED, seed & 0x3ff);
}

// tg3_reset_hw() after the chip reset, with Linux's values for the BCM5764M.
int
Tg3::start() {
    rxReturnNext = 0;
    rxProducer = RX_POSTED;
    txProducer = 0;
    txConsumer = 0;
    tracedRx = tracedTx = 0;
    Memory::set((void *)rxReturnRing, 0, RX_RETURN_COUNT * sizeof(RxDescriptor));
    Memory::set((void *)txRing, 0, TX_COUNT * sizeof(TxDescriptor));
    Memory::set((void *)status, 0, sizeof(Status));
    write(MAILBOX_INTERRUPT_0, 1);

    // tg3_reset_hw(): the 5784 A0 and A1 leave the link aware and link idle modes off and their MAC clocks at 6.25 MHz
    if ((chipRevision >> 8) == CHIPREV_5784_AX) {
        write(CPMU_CTRL, read(CPMU_CTRL) & ~(CPMU_CTRL_LINK_AWARE_MODE | CPMU_CTRL_LINK_IDLE_MODE));
        write(CPMU_LSPD_10MB_CLK, (read(CPMU_LSPD_10MB_CLK) & ~CPMU_MACCLK_MASK) | CPMU_MACCLK_6_25);
        write(CPMU_LNK_AWARE_PWRMD, (read(CPMU_LNK_AWARE_PWRMD) & ~CPMU_MACCLK_MASK) | CPMU_MACCLK_6_25);
        write(CPMU_HST_ACC, (read(CPMU_HST_ACC) & ~CPMU_MACCLK_MASK) | CPMU_MACCLK_6_25);
    }

    write(GRC_MODE, GRC_MODE_VALUE);
    uint32_t misc = read(GRC_MISC_CFG);
    write(GRC_MISC_CFG, (misc & ~(GRC_MISC_CFG_PRESCALAR_MASK | GRC_MISC_CFG_CORECLK_RESET)) |
                            GRC_MISC_CFG_PRESCALAR_66MHZ);

    // Buffer manager
    write(BUFMGR_MB_RDMA_LOW_WATER, 0);
    write(BUFMGR_MB_MACRX_LOW_WATER, 0x10);
    write(BUFMGR_MB_HIGH_WATER, 0x60);
    write(BUFMGR_DMA_LOW_WATER, 0);
    write(BUFMGR_DMA_HIGH_WATER, 0);
    write(BUFMGR_MODE, MODE_ENABLE | MODE_ATTN_ENABLE);
    if (!waitBit(BUFMGR_MODE, MODE_ENABLE, true, 2000)) {
        kprintf("Network: tg3: the buffer manager does not start (BUFMGR_MODE 0x%08x)\n", read(BUFMGR_MODE));
        return -Err::IO;
    }
    write(FTQ_RESET, 0xffffffff);
    write(FTQ_RESET, 0);

    // Standard receive ring, in host memory, 512 entries
    uint64_t rx = (uint64_t)rxRing;
    write(RCVDBDI_STD_BD, (uint32_t)(rx >> 32));
    write(RCVDBDI_STD_BD + 4, (uint32_t)rx);
    write(RCVDBDI_STD_BD + 8, RX_COUNT << 16);
    write(RCVDBDI_STD_BD + 12, NIC_SRAM_RX_BUFFER_DESC);
    write(RCVBDI_STD_THRESH, 0x10);

    // Send ring 0 in host memory, the others disabled
    writeRcb(NIC_SRAM_SEND_RCB, (uint64_t)txRing, TX_COUNT << 16, NIC_SRAM_TX_BUFFER_DESC);
    for (unsigned i = 1; i < 4; i++) {
        writeMemory(NIC_SRAM_SEND_RCB + i * 16 + 8, BDINFO_DISABLED);
    }
    write(MAILBOX_SNDHOST_PROD_IDX_0, 0);
    write(MAILBOX_SNDNIC_PROD_IDX_0, 0);

    // Receive return ring 0, the others disabled
    writeRcb(NIC_SRAM_RCV_RET_RCB, (uint64_t)rxReturnRing, RX_RETURN_COUNT << 16, 0);
    for (unsigned i = 1; i < 4; i++) {
        writeMemory(NIC_SRAM_RCV_RET_RCB + i * 16 + 8, BDINFO_DISABLED);
    }
    write(MAILBOX_RCVRET_CON_IDX_0, 0);
    write(MAILBOX_RCV_STD_PROD_IDX, rxProducer);
    kprintf("Network: tg3 rings: send RCB %08x %08x %08x, receive return RCB %08x %08x %08x (tx ring 0x%x%08x, "
            "return ring 0x%x%08x)\n", readMemory(NIC_SRAM_SEND_RCB), readMemory(NIC_SRAM_SEND_RCB + 4),
            readMemory(NIC_SRAM_SEND_RCB + 8), readMemory(NIC_SRAM_RCV_RET_RCB), readMemory(NIC_SRAM_RCV_RET_RCB + 4),
            readMemory(NIC_SRAM_RCV_RET_RCB + 8), (uint32_t)((uint64_t)txRing >> 32), (uint32_t)(uint64_t)txRing,
            (uint32_t)((uint64_t)rxReturnRing >> 32), (uint32_t)(uint64_t)rxReturnRing);

    // MAC addresses, frame sizes and receive list placement
    setMacAddress(mac());
    write(MAC_RX_MTU_SIZE, ETHERNET_MTU + 14 + 4 + 4);
    write(MAC_TX_LENGTHS, MAC_TX_LENGTHS_VALUE);
    write(RCVLPC_CONFIG, 0x181);
    write(RCVLPC_STATSCTRL, 1);
    write(RCVLPC_STATS_ENABLE, 0x780003);

    // Host coalescing: an interrupt after 5 frames or 20 µs
    write(HOSTCC_MODE, 0);
    waitBit(HOSTCC_MODE, MODE_ENABLE, false, 2000);
    write(HOSTCC_RXCOL_TICKS, 20);
    write(HOSTCC_TXCOL_TICKS, 72);
    write(HOSTCC_RXMAX_FRAMES, 5);
    write(HOSTCC_TXMAX_FRAMES, 53);
    write(HOSTCC_RXCOAL_TICK_INT, 0);
    write(HOSTCC_TXCOAL_TICK_INT, 0);
    write(HOSTCC_RXCOAL_MAXF_INT, 5);
    write(HOSTCC_TXCOAL_MAXF_INT, 5);
    write(HOSTCC_STAT_COAL_TICKS, 0);
    uint64_t block = (uint64_t)status;
    write(HOSTCC_STATUS_BLK_HOST_ADDR, (uint32_t)(block >> 32));
    write(HOSTCC_STATUS_BLK_HOST_ADDR + 4, (uint32_t)block);
    write(HOSTCC_MODE, HOSTCC_MODE_VALUE);
    write(RCVCC_MODE, MODE_ENABLE | MODE_ATTN_ENABLE);
    write(RCVLPC_MODE, MODE_ENABLE);

    write(MAC_MODE, MAC_MODE_RUNNING | MAC_MODE_PORT_GMII | MAC_MODE_CLEAR_STATISTICS);
    delayMs(1);
    write(MAC_MODE, MAC_MODE_RUNNING | MAC_MODE_PORT_GMII);
    write(GRC_LOCAL_CTRL, GRC_LOCAL_CTRL_VALUE);

    // DMA engines and the send and receive state machines
    write(WDMAC_MODE, WDMAC_MODE_VALUE);
    write(RDMAC_MODE, RDMAC_MODE_VALUE);
    write(RCVDCC_MODE, MODE_ENABLE | MODE_ATTN_ENABLE);
    write(SNDDATAC_MODE, MODE_ENABLE);
    write(SNDBDC_MODE, MODE_ENABLE | MODE_ATTN_ENABLE);
    write(RCVBDI_MODE, MODE_ENABLE | MODE_ATTN_ENABLE);
    write(RCVDBDI_MODE, 0x12);
    write(SNDDATAI_MODE, 0xa);
    write(SNDBDI_MODE, MODE_ENABLE | MODE_ATTN_ENABLE);
    write(SNDBDS_MODE, MODE_ENABLE | MODE_ATTN_ENABLE);
    write(MAC_TX_MODE, MAC_TX_MODE_VALUE);
    write(MAC_RX_MODE, MAC_RX_MODE_VALUE);
    write(MAC_MI_MODE, MAC_MI_MODE_VALUE);

    // Every multicast frame, and Linux's two receive rules
    for (unsigned i = 0; i < 4; i++) {
        write(MAC_HASH_0 + i * 4, 0xffffffff);
    }
    write(MAC_RCV_RULE_0, 0xc2000000 & RCV_RULE_DISABLE_MASK);
    write(MAC_RCV_VALUE_0, 0xffffffff & RCV_RULE_DISABLE_MASK);
    write(MAC_RCV_RULE_0 + 8, 0x86000004 & RCV_RULE_DISABLE_MASK);
    write(MAC_RCV_VALUE_0 + 8, 0xffffffff & RCV_RULE_DISABLE_MASK);
    for (unsigned i = 2; i < RCV_RULES; i++) {
        write(MAC_RCV_RULE_0 + i * 8, 0);
        write(MAC_RCV_VALUE_0 + i * 8, 0);
    }
    write(MAC_RCV_RULE_CFG, 8);
    write(MAC_LOW_WMARK_MAX_RX_FRAME, 2);

    // Interrupts: MSI without one shot mode, the PCI interrupt unmasked
    write(MSGINT_MODE, MSGINT_MODE_ENABLE_ONE_SHOT_OFF);
    Pci::write32(at, PCI_MISC_HOST_CTRL, MISC_HOST_CTRL_WORD_SWAP | MISC_HOST_CTRL_PCISTATE_RW |
                                             MISC_HOST_CTRL_INDIR_ACCESS);
    asm volatile("" : : : "memory");
    write(MAILBOX_INTERRUPT_0, 0);
    linkUp = false;
    linkCheckedMs = 0;
    updateLink();
    dump("started");
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
    dump("stopped");
}

void
Tg3::dump(const char *when) const {
    klogf("Network: tg3 %s: MAC_MODE 0x%08x MAC_STATUS 0x%08x TX_STATUS 0x%08x RX_STATUS 0x%08x GRC_MODE "
            "0x%08x\n", when, read(MAC_MODE), read(MAC_STATUS), read(MAC_TX_STATUS), read(MAC_RX_STATUS),
            read(GRC_MODE));
    klogf("Network: tg3 %s: BUFMGR 0x%08x RDMAC 0x%08x/0x%08x WDMAC 0x%08x/0x%08x HOSTCC 0x%08x/0x%08x "
            "MSGINT 0x%08x\n", when, read(BUFMGR_MODE), read(RDMAC_MODE), read(RDMAC_STATUS), read(WDMAC_MODE),
            read(WDMAC_STATUS), read(HOSTCC_MODE), read(HOSTCC_STATUS), read(MSGINT_MODE));
    klogf("Network: tg3 %s: status flags 0x%x, rx producer %u (driver at %u), tx consumer %u (producer %u), "
            "%u interrupts, link %s\n", when, status->flags, (uint32_t)status->indices[0].rxProducer, rxReturnNext,
            (uint32_t)status->indices[0].txConsumer, txProducer, interrupts, linkUp ? "up" : "down");
}

// What Linux's tg3_process_error() looks at, with the rings as the chip sees them.
void
Tg3::dumpError() const {
    klogf("Network: tg3 error: FLOW_ATTN 0x%08x MSGINT_STATUS 0x%08x RDMAC 0x%08x WDMAC 0x%08x SNDDATAI 0x%08x "
            "SNDBDS 0x%08x SNDBDI 0x%08x SNDBDC 0x%08x SNDDATAC 0x%08x\n", read(FLOW_ATTN), read(MSGINT_STATUS),
            read(RDMAC_STATUS), read(WDMAC_STATUS), read(SNDDATAI_MODE + 4), read(SNDBDS_MODE + 4),
            read(SNDBDI_MODE + 4), read(SNDBDC_MODE + 4), read(SNDDATAC_MODE + 4));
    klogf("Network: tg3 error: send RCB %08x %08x %08x %08x, receive return RCB %08x %08x %08x %08x\n",
            readMemory(NIC_SRAM_SEND_RCB), readMemory(NIC_SRAM_SEND_RCB + 4), readMemory(NIC_SRAM_SEND_RCB + 8),
            readMemory(NIC_SRAM_SEND_RCB + 12), readMemory(NIC_SRAM_RCV_RET_RCB),
            readMemory(NIC_SRAM_RCV_RET_RCB + 4), readMemory(NIC_SRAM_RCV_RET_RCB + 8),
            readMemory(NIC_SRAM_RCV_RET_RCB + 12));
    uint64_t tx = (uint64_t)txRing;
    uint64_t rx = (uint64_t)rxRing;
    klogf("Network: tg3 error: tx ring 0x%x%08x, tx buffers 0x%x%08x, rx ring 0x%x%08x, status 0x%x%08x; tx BD 0: "
            "%08x %08x %08x %08x; mailboxes tx %u, NIC tx consumer %u\n", (uint32_t)(tx >> 32), (uint32_t)tx,
            (uint32_t)((uint64_t)txBuffers >> 32), (uint32_t)(uint64_t)txBuffers, (uint32_t)(rx >> 32),
            (uint32_t)rx, (uint32_t)((uint64_t)status >> 32), (uint32_t)(uint64_t)status, txRing[0].addressHigh,
            txRing[0].addressLow, txRing[0].lengthFlags, txRing[0].vlanTag, read(MAILBOX_SNDHOST_PROD_IDX_0),
            (uint32_t)status->indices[0].txConsumer);
}

// The PHY's link bit (read twice: it latches a loss), checked at most every half second. When the link comes up the
// MAC port follows the speed and duplex the PHY negotiated.
void
Tg3::updateLink() const {
    LockGuard guard(phyLock);
    uint64_t now = Clock::uptimeMs();
    if (linkCheckedMs != 0 && now - linkCheckedMs < 500) {
        return;
    }
    linkCheckedMs = now;
    uint16_t bmsr = 0;
    readPhy(MII_BMSR, bmsr);
    if (!readPhy(MII_BMSR, bmsr) || bmsr == 0 || bmsr == 0xFFFF) {
        // A failed read: the BMSR of a PHY always shows its abilities. The link is checked again next time.
        if (!readFailureLogged) {
            readFailureLogged = true;
            klogf("Network: tg3: BMSR read 0x%04x ignored (MI_COM 0x%08x)\n", (uint32_t)bmsr, read(MAC_MI_COM));
        }
        return;
    }
    bool up = bmsr & BMSR_LINK;
    if (up == linkUp) {
        return;
    }
    linkUp = up;
    if (!up) {
        klogf("Network: tg3: link down (BMSR 0x%04x)\n", (uint32_t)bmsr);
        return;
    }
    uint16_t aux = 0;
    readPhy(MII_AUX_STAT, aux);
    unsigned speed = 0;
    bool full = false;
    switch ((aux >> 8) & 7) {
    case 1: speed = 10; break;
    case 2: speed = 10; full = true; break;
    case 3: speed = 100; break;
    case 5: speed = 100; full = true; break;
    case 6: speed = 1000; break;
    case 7: speed = 1000; full = true; break;
    default: break;
    }
    uint32_t mode = MAC_MODE_RUNNING | (speed == 1000 ? MAC_MODE_PORT_GMII : MAC_MODE_PORT_MII) |
                    (full ? 0 : MAC_MODE_HALF_DUPLEX);
    write(MAC_MODE, mode);
    klogf("Network: tg3: link up at %u Mb/s, %s duplex (PHY aux status 0x%04x), MAC_MODE 0x%08x\n", speed,
            full ? "full" : "half", (uint32_t)aux, mode);
}

bool
Tg3::hasLink() const {
    updateLink();
    return linkUp;
}

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
    uint8_t *buffer = txBuffers + txProducer * BUFFER_SIZE;
    Memory::copy(buffer, frame->data(), length);
    PacketBuffer::release(frame);
    if (length < MIN_FRAME) {
        Memory::set(buffer + length, 0, MIN_FRAME - length);
        length = MIN_FRAME;
    }
    if (tracedTx < TRACED_FRAMES) {
        tracedTx++;
        klogf("Network: tg3 tx #%u: %u bytes, type 0x%02x%02x, slot %u\n", tracedTx, (uint32_t)length,
                buffer[12], buffer[13], txProducer);
    }
    uint64_t address = (uint64_t)buffer;
    txRing[txProducer].addressHigh = (uint32_t)(address >> 32);
    txRing[txProducer].addressLow = (uint32_t)address;
    txRing[txProducer].lengthFlags = (uint32_t)length << 16 | TXD_FLAG_END;
    txRing[txProducer].vlanTag = 0;
    txProducer = next;
    asm volatile("" : : : "memory");
    write(MAILBOX_SNDHOST_PROD_IDX_0, txProducer);
    return 0;
}

void
Tg3::retireTransmit() {
    txConsumer = status->indices[0].txConsumer % TX_COUNT;
}

void
Tg3::drainReceiveRing() {
    unsigned produced = status->indices[0].rxProducer % RX_RETURN_COUNT;
    while (rxReturnNext != produced) {
        volatile RxDescriptor &returned = rxReturnRing[rxReturnNext];
        unsigned index = returned.opaque & 0xFFFF;
        unsigned length = returned.indexLength & 0xFFFF;
        uint32_t errors = returned.errorVlan & RXD_ERROR_MASK;
        bool good = index < RX_COUNT && length >= Ethernet::HEADER_SIZE + 4 && length <= RX_DMA_SIZE &&
                    !(returned.typeFlags & RXD_FLAG_ERROR) && (errors == 0 || errors == RXD_ERROR_ODD_NIBBLE);
        if (tracedRx < TRACED_FRAMES) {
            tracedRx++;
            const uint8_t *data = index < RX_COUNT ? rxBuffers + index * BUFFER_SIZE : nullptr;
            klogf("Network: tg3 rx #%u: %u bytes, buffer %u, flags 0x%08x, errors 0x%08x, type 0x%02x%02x%s\n",
                    tracedRx, length, index, returned.typeFlags, returned.errorVlan, data ? data[12] : 0,
                    data ? data[13] : 0, good ? "" : " (dropped)");
        }
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
        rxProducer = (rxProducer + 1) % RX_COUNT;
        rxReturnNext = (rxReturnNext + 1) % RX_RETURN_COUNT;
    }
    asm volatile("" : : : "memory");
    write(MAILBOX_RCVRET_CON_IDX_0, rxReturnNext);
    write(MAILBOX_RCV_STD_PROD_IDX, rxProducer);
}

void
Tg3::handleInterrupt() {
    write(MAILBOX_INTERRUPT_0, 1);
    interrupts++;
    if (interrupts == 1) {
        dump("first interrupt");
    }
    if ((status->flags & STATUS_ERROR) && errorsTraced < TRACED_ERRORS) {
        errorsTraced++;
        dumpError();
    }
    status->flags &= ~STATUS_UPDATED;
    drainReceiveRing();
    {
        LockGuard guard(txLock);
        retireTransmit();
    }
    write(MAILBOX_INTERRUPT_0, 0);
    if (status->flags & STATUS_UPDATED) {
        write(GRC_LOCAL_CTRL, GRC_LOCAL_CTRL_VALUE | GRC_LCLCTRL_SETINT);
    }
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
