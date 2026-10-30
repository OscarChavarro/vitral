#ifndef __RTL8169__
#define __RTL8169__
#include "drivers/pci/Pci.h"
#include "kernel/TrapFrame.h"
#include "lib/vitralKernel/SpinLock.h"
#include "net/NetDevice.h"

class Rtl8169 : public NetDevice {
public:
    static bool init();

    const char *driver() const override { return "rtl8169"; }
    bool hasLink() const override;

protected:
    int start() override;
    void stop() override;
    int send(PacketBuffer *frame) override;

private:
    static constexpr uint32_t MAC0 = 0x00;
    static constexpr uint32_t TX_DESC_LOW = 0x20;
    static constexpr uint32_t TX_DESC_HIGH = 0x24;
    static constexpr uint32_t CHIP_COMMAND = 0x37;
    static constexpr uint32_t TX_POLL = 0x38;
    static constexpr uint32_t INTERRUPT_MASK = 0x3c;
    static constexpr uint32_t INTERRUPT_STATUS = 0x3e;
    static constexpr uint32_t TX_CONFIG = 0x40;
    static constexpr uint32_t RX_CONFIG = 0x44;
    static constexpr uint32_t CONFIG_9346 = 0x50;
    static constexpr uint32_t PHY_STATUS = 0x6c;
    static constexpr uint32_t RX_DESC_LOW = 0xe4;
    static constexpr uint32_t RX_DESC_HIGH = 0xe8;
    static constexpr uint8_t COMMAND_RX_ENABLE = 0x08;
    static constexpr uint8_t COMMAND_TX_ENABLE = 0x04;
    static constexpr uint8_t COMMAND_RESET = 0x10;
    static constexpr uint8_t CONFIG_9346_UNLOCK = 0xc0;
    static constexpr uint8_t CONFIG_9346_LOCK = 0x00;
    static constexpr uint8_t PHY_LINK_UP = 0x02;
    static constexpr uint8_t TX_POLL_NORMAL = 0x40;
    static constexpr uint16_t INTERRUPT_RX_OK = 0x0001;
    static constexpr uint16_t INTERRUPT_RX_ERROR = 0x0002;
    static constexpr uint16_t INTERRUPT_TX_ERROR = 0x0008;
    static constexpr uint16_t INTERRUPT_RX_OVERFLOW = 0x0010;
    static constexpr uint16_t INTERRUPT_RX_FIFO_OVERFLOW = 0x0040;
    static constexpr uint32_t RX_ACCEPT_ALL_PHYSICAL = 1u << 0;
    static constexpr uint32_t RX_ACCEPT_PHYSICAL = 1u << 1;
    static constexpr uint32_t RX_ACCEPT_MULTICAST = 1u << 2;
    static constexpr uint32_t RX_ACCEPT_BROADCAST = 1u << 3;
    static constexpr uint32_t RX_DMA_BURST_UNLIMITED = 7u << 8;
    static constexpr uint32_t RX_BUFFER_8K = 0u << 11;
    static constexpr uint32_t TX_DMA_BURST_1024 = 6u << 8;
    static constexpr uint32_t TX_INTERFRAME_GAP = 3u << 24;
    static constexpr uint32_t DESC_OWN = 1u << 31;
    static constexpr uint32_t DESC_END = 1u << 30;
    static constexpr uint32_t DESC_FIRST = 1u << 29;
    static constexpr uint32_t DESC_LAST = 1u << 28;
    static constexpr uint32_t DESC_RX_ERROR = 1u << 20;
    static constexpr uint32_t DESC_LENGTH = 0x3fffu;
    static constexpr unsigned RX_COUNT = 64;
    static constexpr unsigned TX_COUNT = 64;
    static constexpr size_t BUFFER_SIZE = 2048;
    static constexpr size_t ETHERNET_FCS = 4;

    class Descriptor {
    public:
        uint32_t options1;
        uint32_t options2;
        uint64_t address;
    } __attribute__((packed));
    static_assert(sizeof(Descriptor) == 16, "RTL8169 descriptor is 16 bytes");

    Rtl8169(volatile uint8_t *registers, uint8_t irqLine);

    static TrapFrame *onIrq(TrapFrame *frame);
    bool reset();
    bool allocateRings();
    void drainReceiveRing();
    void handleInterrupt();
    uint32_t read32(uint32_t reg) const { return *(volatile uint32_t *)(mmio + reg); }
    uint16_t read16(uint32_t reg) const { return *(volatile uint16_t *)(mmio + reg); }
    uint8_t read8(uint32_t reg) const { return *(volatile uint8_t *)(mmio + reg); }
    void write32(uint32_t reg, uint32_t value) { *(volatile uint32_t *)(mmio + reg) = value; }
    void write16(uint32_t reg, uint16_t value) { *(volatile uint16_t *)(mmio + reg) = value; }
    void write8(uint32_t reg, uint8_t value) { *(volatile uint8_t *)(mmio + reg) = value; }

    volatile uint8_t *mmio;
    uint8_t irq;
    volatile Descriptor *rxRing;
    volatile Descriptor *txRing;
    uint8_t *rxBuffers;
    uint8_t *txBuffers;
    unsigned rxNext;
    unsigned txNext;
    SpinLock txLock;

    static Rtl8169 *instance;
};

#endif
