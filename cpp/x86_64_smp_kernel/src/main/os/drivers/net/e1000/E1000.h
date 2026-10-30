#ifndef __E1000__
#define __E1000__
#include "drivers/pci/Pci.h"
#include "kernel/TrapFrame.h"
#include "lib/vitralKernel/SpinLock.h"
#include "net/NetDevice.h"

class E1000 : public NetDevice {
public:
    static bool init();

    const char *driver() const override { return "e1000"; }
    uint16_t mtu() const override { return ETHERNET_MTU; }
    bool hasLink() const override { return linkUp(); }
    bool linkUp() const;
    unsigned linkMbps() const;

protected:
    int start() override;
    void stop() override;
    int send(PacketBuffer *frame) override;

private:
    static constexpr uint32_t CTRL = 0x0000;
    static constexpr uint32_t STATUS = 0x0008;
    static constexpr uint32_t CTRL_EXT = 0x0018;
    static constexpr uint32_t EERD = 0x0014;
    static constexpr uint32_t ICR = 0x00C0;
    static constexpr uint32_t IMS = 0x00D0;
    static constexpr uint32_t IMC = 0x00D8;
    static constexpr uint32_t RCTL = 0x0100;
    static constexpr uint32_t TCTL = 0x0400;
    static constexpr uint32_t TIPG = 0x0410;
    static constexpr uint32_t RDBAL = 0x2800;
    static constexpr uint32_t RDBAH = 0x2804;
    static constexpr uint32_t RDLEN = 0x2808;
    static constexpr uint32_t RDH = 0x2810;
    static constexpr uint32_t RDT = 0x2818;
    static constexpr uint32_t TDBAL = 0x3800;
    static constexpr uint32_t TDBAH = 0x3804;
    static constexpr uint32_t TDLEN = 0x3808;
    static constexpr uint32_t TDH = 0x3810;
    static constexpr uint32_t TDT = 0x3818;
    static constexpr uint32_t MTA = 0x5200;
    static constexpr uint32_t RAL0 = 0x5400;
    static constexpr uint32_t RAH0 = 0x5404;
    static constexpr uint32_t CTRL_ASDE = 1u << 5;
    static constexpr uint32_t CTRL_SLU = 1u << 6;
    static constexpr uint32_t CTRL_LRST = 1u << 3;
    static constexpr uint32_t CTRL_ILOS = 1u << 7;
    static constexpr uint32_t CTRL_RST = 1u << 26;
    static constexpr uint32_t CTRL_PHY_RST = 1u << 31;
    static constexpr uint32_t CTRL_MEHE = 1u << 19;
    static constexpr uint32_t CTRL_EXT_DRV_LOAD = 1u << 28;
    static constexpr uint32_t STATUS_LU = 1u << 1;
    static constexpr uint32_t STATUS_LAN_INIT_DONE = 1u << 9;
    static constexpr uint32_t STATUS_SPEED_MASK = 3u << 6;
    static constexpr uint32_t INT_LSC = 1u << 2;
    static constexpr uint32_t INT_RXDMT0 = 1u << 4;
    static constexpr uint32_t INT_RXO = 1u << 6;
    static constexpr uint32_t INT_RXT0 = 1u << 7;
    static constexpr uint32_t RCTL_EN = 1u << 1;
    static constexpr uint32_t RCTL_BAM = 1u << 15;
    static constexpr uint32_t RCTL_SECRC = 1u << 26;
    static constexpr uint32_t TCTL_EN = 1u << 1;
    static constexpr uint32_t TCTL_PSP = 1u << 3;
    static constexpr uint32_t TCTL_CT = 0x0Fu << 4;
    static constexpr uint32_t TCTL_COLD = 0x40u << 12;
    static constexpr uint32_t TIPG_DEFAULT = 0x0060200A;
    static constexpr uint8_t RX_DD = 1;
    static constexpr uint8_t RX_EOP = 2;
    static constexpr uint8_t TX_EOP = 1;
    static constexpr uint8_t TX_IFCS = 2;
    static constexpr uint8_t TX_RS = 8;
    static constexpr uint8_t TX_DD = 1;
    static constexpr uint32_t EERD_START = 1;
    static constexpr uint32_t EERD_DONE = 1u << 4;
    static constexpr uint32_t RAH_AV = 1u << 31;

    static constexpr uint8_t MSI_VECTOR = 0x50;

    static constexpr unsigned RX_COUNT = 32;
    static constexpr unsigned TX_COUNT = 32;
    static constexpr size_t BUFFER_SIZE = 2048;

    class RxDescriptor {
    public:
        uint64_t address;
        uint16_t length;
        uint16_t checksum;
        uint8_t status;
        uint8_t errors;
        uint16_t special;
    } __attribute__((packed));
    class TxDescriptor {
    public:
        uint64_t address;
        uint16_t length;
        uint8_t cso;
        uint8_t cmd;
        uint8_t status;
        uint8_t css;
        uint16_t special;
    } __attribute__((packed));
    static_assert(sizeof(RxDescriptor) == 16 && sizeof(TxDescriptor) == 16, "descriptors are 16 bytes");

    E1000(volatile uint8_t *registers, uint8_t irqLine, bool pch);

    static TrapFrame *onIrq(TrapFrame *frame);
    void handleInterrupt();
    void drainReceiveRing();

    uint32_t read(uint32_t reg) const { return *(volatile uint32_t *)(mmio + reg); }
    void write(uint32_t reg, uint32_t value) { *(volatile uint32_t *)(mmio + reg) = value; }
    bool reset();
    bool resetPch();
    bool readMac(MacAddress &out);
    bool readEeprom(uint8_t word, uint16_t &value);
    bool allocateRings();

    volatile uint8_t *mmio;
    uint8_t irq;
    bool msi;
    bool pch;
    volatile RxDescriptor *rxRing;
    volatile TxDescriptor *txRing;
    uint8_t *rxBuffers;
    uint8_t *txBuffers;
    unsigned rxNext;
    unsigned txNext;
    SpinLock txLock;

    static E1000 *instance;
};

#endif
