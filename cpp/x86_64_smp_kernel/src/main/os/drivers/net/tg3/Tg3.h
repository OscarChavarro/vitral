#ifndef __TG3__
#define __TG3__
#include "drivers/pci/Pci.h"
#include "kernel/TrapFrame.h"
#include "lib/vitralKernel/SpinLock.h"
#include "net/NetDevice.h"

// Broadcom NetXtreme BCM5764M (Tigon 3, 5784 family, PCI Express), as Linux's tg3 driver sets it up: the chip reset
// of tg3_chip_reset() and the initialization of tg3_reset_hw(), reduced to one standard receive ring, one send ring
// in host memory, one receive return ring and the non-tagged status block, with MSI. The register values follow those
// Linux leaves in the chip (read from a BCM5764M driven by Linux 6.x); the descriptor and status block layouts are
// Linux's for a little-endian host (GRC_MODE swaps the non-frame words).
//
// For debugging, the driver writes to the kernel log: each step of the reset, the registers once the chip runs, the
// link changes and the first frames received and sent.
class Tg3 : public NetDevice {
public:
    static bool init();

    const char *driver() const override { return "tg3"; }
    bool hasLink() const override;

protected:
    int start() override;
    void stop() override;
    int send(PacketBuffer *frame) override;

private:
    // PCI configuration registers, also seen at the start of the register space
    static constexpr uint8_t PCI_MISC_HOST_CTRL = 0x68;
    static constexpr uint8_t PCI_DMA_RW_CTRL = 0x6c;
    static constexpr uint32_t MISC_HOST_CTRL_MASK_PCI_INT = 0x2;
    static constexpr uint32_t MISC_HOST_CTRL_WORD_SWAP = 0x8;
    static constexpr uint32_t MISC_HOST_CTRL_PCISTATE_RW = 0x10;
    static constexpr uint32_t MISC_HOST_CTRL_INDIR_ACCESS = 0x80;
    static constexpr uint32_t DMA_RW_CTRL_PCIE = 0x76180000;
    static constexpr uint8_t PCI_CAPABILITY_PCIE = 0x10;
    static constexpr uint8_t PCIE_DEVICE_CONTROL = 8;
    static constexpr uint16_t PCIE_RELAXED_ORDERING = 0x10;
    static constexpr uint16_t PCIE_NO_SNOOP = 0x800;
    static constexpr uint32_t GRC_MEM_WIN_BASE = 0x7c;
    static constexpr uint32_t GRC_MEM_WIN_DATA = 0x84;

    // The mailboxes are 64 bit registers: the value goes in their low half, at +4
    static constexpr uint32_t MAILBOX_INTERRUPT_0 = 0x200 + 4;
    static constexpr uint32_t MAILBOX_RCV_STD_PROD_IDX = 0x268 + 4;
    static constexpr uint32_t MAILBOX_RCVRET_CON_IDX_0 = 0x280 + 4;
    static constexpr uint32_t MAILBOX_SNDHOST_PROD_IDX_0 = 0x300 + 4;
    static constexpr uint32_t MAILBOX_SNDNIC_PROD_IDX_0 = 0x380 + 4;

    static constexpr uint32_t MAC_MODE = 0x400;
    static constexpr uint32_t MAC_STATUS = 0x404;
    static constexpr uint32_t MAC_EVENT = 0x408;
    static constexpr uint32_t MAC_ADDR_0_HIGH = 0x410;
    static constexpr uint32_t MAC_ADDR_0_LOW = 0x414;
    static constexpr uint32_t MAC_TX_BACKOFF_SEED = 0x438;
    static constexpr uint32_t MAC_RX_MTU_SIZE = 0x43c;
    static constexpr uint32_t MAC_MI_COM = 0x44c;
    static constexpr uint32_t MAC_MI_MODE = 0x454;
    static constexpr uint32_t MAC_AUTO_POLL = 0x458;
    static constexpr uint32_t MAC_TX_MODE = 0x45c;
    static constexpr uint32_t MAC_TX_STATUS = 0x460;
    static constexpr uint32_t MAC_TX_LENGTHS = 0x464;
    static constexpr uint32_t MAC_RX_MODE = 0x468;
    static constexpr uint32_t MAC_RX_STATUS = 0x46c;
    static constexpr uint32_t MAC_HASH_0 = 0x470;
    static constexpr uint32_t MAC_RCV_RULE_0 = 0x480;
    static constexpr uint32_t MAC_RCV_VALUE_0 = 0x484;
    static constexpr uint32_t MAC_RCV_RULE_CFG = 0x500;
    static constexpr uint32_t MAC_LOW_WMARK_MAX_RX_FRAME = 0x504;
    static constexpr uint32_t SNDDATAI_MODE = 0xc00;
    static constexpr uint32_t SNDDATAC_MODE = 0x1000;
    static constexpr uint32_t SNDBDS_MODE = 0x1400;
    static constexpr uint32_t SNDBDI_MODE = 0x1800;
    static constexpr uint32_t SNDBDC_MODE = 0x1c00;
    static constexpr uint32_t RCVLPC_MODE = 0x2000;
    static constexpr uint32_t RCVLPC_CONFIG = 0x2010;
    static constexpr uint32_t RCVLPC_STATSCTRL = 0x2014;
    static constexpr uint32_t RCVLPC_STATS_ENABLE = 0x2018;
    static constexpr uint32_t RCVDBDI_MODE = 0x2400;
    static constexpr uint32_t RCVDBDI_STD_BD = 0x2450;
    static constexpr uint32_t RCVDCC_MODE = 0x2800;
    static constexpr uint32_t RCVBDI_MODE = 0x2c00;
    static constexpr uint32_t RCVBDI_STD_THRESH = 0x2c18;
    static constexpr uint32_t RCVCC_MODE = 0x3000;
    static constexpr uint32_t HOSTCC_MODE = 0x3c00;
    static constexpr uint32_t HOSTCC_STATUS = 0x3c04;
    static constexpr uint32_t HOSTCC_RXCOL_TICKS = 0x3c08;
    static constexpr uint32_t HOSTCC_TXCOL_TICKS = 0x3c0c;
    static constexpr uint32_t HOSTCC_RXMAX_FRAMES = 0x3c10;
    static constexpr uint32_t HOSTCC_TXMAX_FRAMES = 0x3c14;
    static constexpr uint32_t HOSTCC_RXCOAL_TICK_INT = 0x3c18;
    static constexpr uint32_t HOSTCC_TXCOAL_TICK_INT = 0x3c1c;
    static constexpr uint32_t HOSTCC_RXCOAL_MAXF_INT = 0x3c20;
    static constexpr uint32_t HOSTCC_TXCOAL_MAXF_INT = 0x3c24;
    static constexpr uint32_t HOSTCC_STAT_COAL_TICKS = 0x3c28;
    static constexpr uint32_t HOSTCC_STATUS_BLK_HOST_ADDR = 0x3c38;
    static constexpr uint32_t MEMARB_MODE = 0x4000;
    static constexpr uint32_t BUFMGR_MODE = 0x4400;
    static constexpr uint32_t BUFMGR_MB_RDMA_LOW_WATER = 0x4410;
    static constexpr uint32_t BUFMGR_MB_MACRX_LOW_WATER = 0x4414;
    static constexpr uint32_t BUFMGR_MB_HIGH_WATER = 0x4418;
    static constexpr uint32_t BUFMGR_DMA_LOW_WATER = 0x4434;
    static constexpr uint32_t BUFMGR_DMA_HIGH_WATER = 0x4438;
    static constexpr uint32_t RDMAC_MODE = 0x4800;
    static constexpr uint32_t RDMAC_STATUS = 0x4804;
    static constexpr uint32_t WDMAC_MODE = 0x4c00;
    static constexpr uint32_t WDMAC_STATUS = 0x4c04;
    static constexpr uint32_t FTQ_RESET = 0x5c00;
    static constexpr uint32_t MSGINT_MODE = 0x6000;
    static constexpr uint32_t GRC_MODE = 0x6800;
    static constexpr uint32_t GRC_MISC_CFG = 0x6804;
    static constexpr uint32_t GRC_LOCAL_CTRL = 0x6808;
    static constexpr uint32_t GRC_FASTBOOT_PC = 0x6894;
    static constexpr uint32_t PCIE_TRANSACTION_CFG = 0x7c00;
    static constexpr uint32_t PCIE_TRANSACTION_CFG_LINUX = 1u << 25;
    static constexpr uint32_t GRC_MISC_CFG_PCIE = 1u << 29;
    static constexpr uint8_t PCI_PRODID_ASICREV = 0xbc;
    static constexpr uint8_t PCIE_DEVICE_STATUS = 0x0a;
    static constexpr uint16_t PCIE_DEVICE_STATUS_ERRORS = 0x000f;
    // The chip power management unit of the 5784 family: its link aware and link idle modes slow the MAC clock
    static constexpr uint32_t CPMU_CTRL = 0x3600;
    static constexpr uint32_t CPMU_CTRL_LINK_IDLE_MODE = 0x200;
    static constexpr uint32_t CPMU_CTRL_LINK_AWARE_MODE = 0x400;
    static constexpr uint32_t CPMU_LSPD_10MB_CLK = 0x3604;
    static constexpr uint32_t CPMU_LNK_AWARE_PWRMD = 0x3610;
    static constexpr uint32_t CPMU_HST_ACC = 0x361c;
    static constexpr uint32_t CPMU_MACCLK_MASK = 0x001f0000;
    static constexpr uint32_t CPMU_MACCLK_6_25 = 0x00130000;
    static constexpr uint32_t ASIC_REV_USE_PROD_ID_REG = 0x0f;
    static constexpr uint32_t CHIPREV_5784_AX = 0x57840;
    static constexpr uint32_t NIC_SRAM_DATA_CFG = 0x0b58;
    static constexpr uint32_t NIC_SRAM_DATA_CFG_ASF_ENABLE = 0x00000080;
    static constexpr uint32_t NIC_SRAM_DATA_CFG_APE_ENABLE = 0x00200000;

    // Values (those of Linux on the BCM5764M)
    static constexpr uint32_t MODE_ENABLE = 2;
    static constexpr uint32_t MODE_ATTN_ENABLE = 4;
    static constexpr uint32_t GRC_MODE_VALUE = 0x04130034;  // swaps, host stack up, host send BDs, IRQ on MAC attn
    static constexpr uint32_t GRC_MISC_CFG_CORECLK_RESET = 0x1;
    static constexpr uint32_t GRC_MISC_CFG_PRESCALAR_MASK = 0xfe;
    static constexpr uint32_t GRC_MISC_CFG_PRESCALAR_66MHZ = 65 << 1;
    static constexpr uint32_t GRC_LOCAL_CTRL_VALUE = 0x01009308;
    static constexpr uint32_t GRC_LCLCTRL_SETINT = 0x4;
    static constexpr uint32_t MAC_MODE_PORT_MII = 0x4;
    static constexpr uint32_t MAC_MODE_PORT_GMII = 0x8;
    static constexpr uint32_t MAC_MODE_HALF_DUPLEX = 0x2;
    static constexpr uint32_t MAC_MODE_RUNNING = 0x00e04800;  // RX and TX statistics, TDE, RDE and FHDE
    static constexpr uint32_t MAC_MODE_CLEAR_STATISTICS = 0x9000;
    static constexpr uint32_t MAC_MI_MODE_VALUE = 0x000c8000;
    static constexpr uint32_t MAC_TX_MODE_VALUE = 0x102;
    static constexpr uint32_t MAC_RX_MODE_VALUE = 0x01000002;
    static constexpr uint32_t MAC_TX_LENGTHS_VALUE = 0x2620;
    static constexpr uint32_t MAC_TX_STATUS_LINK_UP = 0x8;
    static constexpr uint32_t RCV_RULE_DISABLE_MASK = 0x7fffffff;
    static constexpr unsigned RCV_RULES = 8;
    static constexpr uint32_t RDMAC_MODE_VALUE = 0x08033bfe;
    static constexpr uint32_t WDMAC_MODE_VALUE = 0x200003fe;
    static constexpr uint32_t HOSTCC_MODE_VALUE = 0x302;
    static constexpr uint32_t MSGINT_MODE_ENABLE_ONE_SHOT_OFF = 0x22;
    static constexpr uint32_t BDINFO_DISABLED = 2;

    // NIC memory
    static constexpr uint32_t NIC_SRAM_SEND_RCB = 0x100;
    static constexpr uint32_t NIC_SRAM_RCV_RET_RCB = 0x200;
    static constexpr uint32_t NIC_SRAM_TX_BUFFER_DESC = 0x4000;
    static constexpr uint32_t NIC_SRAM_RX_BUFFER_DESC = 0x6000;
    static constexpr uint32_t NIC_SRAM_FIRMWARE_MBOX = 0x0b50;
    static constexpr uint32_t FIRMWARE_MBOX_MAGIC = 0x4B657654;
    static constexpr uint32_t NIC_SRAM_MAC_ADDR_HIGH = 0x0c14;
    static constexpr uint32_t NIC_SRAM_MAC_ADDR_LOW = 0x0c18;

    // PHY (at MII address 1)
    static constexpr unsigned PHY_ADDRESS = 1;
    static constexpr unsigned MII_BMCR = 0;
    static constexpr unsigned MII_BMSR = 1;
    static constexpr unsigned MII_ADVERTISE = 4;
    static constexpr unsigned MII_CTRL1000 = 9;
    static constexpr unsigned MII_AUX_STAT = 0x19;
    static constexpr uint16_t BMSR_LINK = 0x4;
    // __tg3_readphy() and __tg3_writephy(): MI_COM polled every 10 microseconds, at most this many times
    static constexpr unsigned PHY_BUSY_LOOPS = 5000;

    // Descriptors and rings
    static constexpr uint32_t TXD_FLAG_END = 0x4;
    static constexpr uint32_t RXD_FLAG_END = 0x4;
    static constexpr uint32_t RXD_FLAG_ERROR = 0x400;
    static constexpr uint32_t RXD_ERROR_MASK = 0xffff0000;
    static constexpr uint32_t RXD_ERROR_ODD_NIBBLE = 0x00800000;
    static constexpr uint32_t RXD_OPAQUE_RING_STD = 0x10000;
    static constexpr uint32_t STATUS_UPDATED = 1;
    static constexpr uint32_t STATUS_ERROR = 4;
    static constexpr uint32_t FLOW_ATTN = 0x3c48;
    static constexpr uint32_t MSGINT_STATUS = 0x6004;
    static constexpr unsigned TRACED_ERRORS = 3;
    static constexpr uint8_t MSI_VECTOR = 0x50;
    static constexpr unsigned RX_COUNT = 512;
    static constexpr unsigned RX_POSTED = 200;
    static constexpr unsigned RX_RETURN_COUNT = 512;
    static constexpr unsigned TX_COUNT = 512;
    static constexpr unsigned BUFFER_SIZE = 2048;
    static constexpr unsigned RX_DMA_SIZE = 1536;
    static constexpr unsigned MIN_FRAME = 60;
    static constexpr unsigned TRACED_FRAMES = 4;

    static constexpr uint32_t RESET_WAIT_MS = 120;
    static constexpr uint32_t FIRMWARE_WAIT_MS = 1000;

    class TxDescriptor {
    public:
        uint32_t addressHigh;
        uint32_t addressLow;
        uint32_t lengthFlags;
        uint32_t vlanTag;
    } __attribute__((packed));
    class RxDescriptor {
    public:
        uint32_t addressHigh;
        uint32_t addressLow;
        uint32_t indexLength;
        uint32_t typeFlags;
        uint32_t checksums;
        uint32_t errorVlan;
        uint32_t reserved;
        uint32_t opaque;
    } __attribute__((packed));
    class Status {
    public:
        uint32_t flags;
        uint32_t tag;
        uint16_t rxJumboConsumer;
        uint16_t rxConsumer;
        uint16_t rxMiniConsumer;
        uint16_t reserved;
        class Index {
        public:
            uint16_t rxProducer;
            uint16_t txConsumer;
        } indices[16];
    } __attribute__((packed));
    static_assert(sizeof(TxDescriptor) == 16, "Tigon 3 send descriptor");
    static_assert(sizeof(RxDescriptor) == 32, "Tigon 3 receive descriptor");
    static_assert(sizeof(Status) == 80, "Tigon 3 status block");

    Tg3(volatile uint8_t *registers, const Pci::Location &where);
    static TrapFrame *onIrq(TrapFrame *frame);
    static Tg3 *instance;

    uint32_t read(uint32_t reg) const { return *(volatile uint32_t *)(mmio + reg); }
    void write(uint32_t reg, uint32_t value) const { *(volatile uint32_t *)(mmio + reg) = value; }
    void writeFlush(uint32_t reg, uint32_t value) const {
        write(reg, value);
        read(reg);
    }
    void writeMemory(uint32_t address, uint32_t value) const;
    uint32_t readMemory(uint32_t address) const;
    void writeRcb(uint32_t address, uint64_t host, uint32_t maxLengthFlags, uint32_t nic) const;
    static void delayMs(uint32_t milliseconds);
    static void pause(unsigned cycles);
    static void delayUs(uint32_t microseconds);
    bool waitBit(uint32_t reg, uint32_t bit, bool set, unsigned microseconds) const;
    bool readMac(MacAddress &out) const;
    bool allocateRings();
    bool reset();
    uint8_t pcieCapability() const;
    bool initializePhy();
    bool readPhy(unsigned reg, uint16_t &value) const;
    bool writePhy(unsigned reg, uint16_t value) const;
    void setMacAddress(const MacAddress &mac);
    void updateLink() const;
    void dump(const char *when) const;
    void dumpError() const;
    void handleInterrupt();
    void drainReceiveRing();
    void retireTransmit();

    volatile uint8_t *mmio;
    Pci::Location at;
    volatile RxDescriptor *rxRing;
    volatile RxDescriptor *rxReturnRing;
    volatile TxDescriptor *txRing;
    volatile Status *status;
    uint8_t *rxBuffers;
    uint8_t *txBuffers;
    unsigned rxReturnNext;
    unsigned rxProducer;
    unsigned txProducer;
    unsigned txConsumer;
    unsigned tracedRx;
    unsigned tracedTx;
    unsigned interrupts;
    unsigned errorsTraced;
    mutable bool linkUp;
    mutable bool readFailureLogged = false;
    mutable uint64_t linkCheckedMs;
    // PHY access (MAC_MI_COM) and the link state: hasLink() is called by every sender, on any CPU, and two MDIO
    // transactions at a time corrupt each other's results (Linux holds tp->lock around them)
    mutable SpinLock phyLock;
    uint32_t chipRevision;
    SpinLock txLock;
};

#endif
