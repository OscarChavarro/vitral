#ifndef __TG3__
#define __TG3__
#include "drivers/pci/Pci.h"
#include "kernel/TrapFrame.h"
#include "lib/vitralKernel/SpinLock.h"
#include "net/NetDevice.h"

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
    static constexpr uint32_t MAILBOX_INTERRUPT_0 = 0x200;
    static constexpr uint32_t MAILBOX_RCV_STD_PROD_IDX = 0x26c;
    static constexpr uint32_t MAILBOX_RCVRET_CON_IDX_0 = 0x284;
    static constexpr uint32_t MAILBOX_SNDHOST_PROD_IDX_0 = 0x304;
    static constexpr uint32_t MAC_MODE = 0x400;
    static constexpr uint32_t MAC_ADDR_0_HIGH = 0x410;
    static constexpr uint32_t MAC_ADDR_0_LOW = 0x414;
    static constexpr uint32_t MAC_MI_STAT = 0x450;
    static constexpr uint32_t MAC_MI_COM = 0x44c;
    static constexpr uint32_t MAC_MI_MODE = 0x454;
    static constexpr uint32_t MAC_TX_MODE = 0x45c;
    static constexpr uint32_t MAC_TX_STATUS = 0x460;
    static constexpr uint32_t MAC_RX_MODE = 0x468;
    static constexpr uint32_t SNDDATAI_MODE = 0xc00;
    static constexpr uint32_t SNDBDS_MODE = 0x1400;
    static constexpr uint32_t SNDBDI_MODE = 0x1800;
    static constexpr uint32_t SNDBDC_MODE = 0x1c00;
    static constexpr uint32_t RCVLPC_MODE = 0x2000;
    static constexpr uint32_t RCVDBDI_MODE = 0x2400;
    static constexpr uint32_t RCVDBDI_STD_BD = 0x2450;
    static constexpr uint32_t RCVDCC_MODE = 0x2800;
    static constexpr uint32_t RCVBDI_MODE = 0x2c00;
    static constexpr uint32_t RCVCC_MODE = 0x3000;
    static constexpr uint32_t HOSTCC_MODE = 0x3c00;
    static constexpr uint32_t HOSTCC_RXCOL_TICKS = 0x3c08;
    static constexpr uint32_t HOSTCC_TXCOL_TICKS = 0x3c0c;
    static constexpr uint32_t HOSTCC_RXMAX_FRAMES = 0x3c10;
    static constexpr uint32_t HOSTCC_TXMAX_FRAMES = 0x3c14;
    static constexpr uint32_t HOSTCC_STATUS_BLK_HOST_ADDR = 0x3c38;
    static constexpr uint32_t BUFMGR_MODE = 0x4400;
    static constexpr uint32_t RDMAC_MODE = 0x4800;
    static constexpr uint32_t WDMAC_MODE = 0x4c00;
    static constexpr uint32_t GRC_MODE = 0x6800;
    static constexpr uint32_t GRC_MISC_CFG = 0x6804;
    static constexpr uint32_t GRC_LOCAL_CTRL = 0x6808;
    static constexpr uint32_t GRC_MEM_WIN_BASE = 0x7c;
    static constexpr uint32_t GRC_MEM_WIN_DATA = 0x80;
    static constexpr uint32_t NIC_SRAM_SEND_RCB = 0x100;
    static constexpr uint32_t NIC_SRAM_RCV_RET_RCB = 0x200;
    static constexpr uint32_t NIC_SRAM_TX_BUFFER_DESC = 0x4000;
    static constexpr uint32_t NIC_SRAM_RX_BUFFER_DESC = 0x6000;
    static constexpr uint32_t MODE_ENABLE = 2;
    static constexpr uint32_t MODE_ATTN_ENABLE = 4;
    static constexpr uint32_t GRC_MODE_HOST_SENDBDS = 0x20000;
    static constexpr uint32_t MAC_MODE_RXSTAT_ENABLE = 0x200000;
    static constexpr uint32_t MAC_MODE_TXSTAT_ENABLE = 0x400000;
    static constexpr uint32_t MAC_MODE_TDE_ENABLE = 0x2000000;
    static constexpr uint32_t MAC_MODE_RDE_ENABLE = 0x4000000;
    static constexpr uint32_t TXD_FLAG_END = 4;
    static constexpr uint32_t MAC_TX_STATUS_LINK_UP = 8;
    static constexpr uint32_t RXD_FLAG_ERROR = 0x400;
    static constexpr uint32_t RXD_OPAQUE_RING_STD = 0x10000;
    static constexpr uint8_t MSI_VECTOR = 0x50;
    static constexpr unsigned RX_COUNT = 512;
    static constexpr unsigned RX_POSTED = 200;
    static constexpr unsigned RX_RETURN_COUNT = 512;
    static constexpr unsigned TX_COUNT = 512;
    static constexpr unsigned BUFFER_SIZE = 2048;

    class Descriptor {
    public:
        uint32_t addressHigh;
        uint32_t addressLow;
        uint32_t first;
        uint32_t second;
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
    static_assert(sizeof(Descriptor) == 16, "Tigon 3 descriptor");
    static_assert(sizeof(Status) == 80, "Tigon 3 status block");

    explicit Tg3(volatile uint8_t *registers);
    static TrapFrame *onIrq(TrapFrame *frame);
    static Tg3 *instance;

    uint32_t read(uint32_t reg) const { return *(volatile uint32_t *)(mmio + reg); }
    void write(uint32_t reg, uint32_t value) { *(volatile uint32_t *)(mmio + reg) = value; }
    void writeMemory(uint32_t address, uint32_t value);
    void writeBdInfo(uint32_t address, uint64_t host, uint32_t maxLength, uint32_t nic);
    bool readMac(MacAddress &out) const;
    bool allocateRings();
    bool reset();
    bool initializePhy();
    bool readPhy(unsigned reg, uint16_t &value);
    bool writePhy(unsigned reg, uint16_t value);
    static void pause(unsigned cycles);
    void handleInterrupt();
    void drainReceiveRing();
    void retireTransmit();

    volatile uint8_t *mmio;
    volatile Descriptor *rxRing;
    volatile Descriptor *rxReturnRing;
    volatile Descriptor *txRing;
    volatile Status *status;
    uint8_t *rxBuffers;
    uint8_t *txBuffers;
    unsigned rxReturnNext;
    unsigned rxProducer;
    unsigned txProducer;
    unsigned txConsumer;
    SpinLock txLock;
};

#endif
