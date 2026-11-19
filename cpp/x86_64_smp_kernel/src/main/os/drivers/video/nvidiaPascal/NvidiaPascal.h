#ifndef __NVIDIA_PASCAL__
#define __NVIDIA_PASCAL__
#include <stdint.h>
#include "drivers/pci/Pci.h"

// The display engine of the NVIDIA Pascal cards (GP10x, chipsets NV130 to NV13B: GeForce GTX 10x0), after nouveau's
// display code (nvkm/engine/disp, gf119 and gp102, and dispnv50 with the 907d class methods). Unlike the GeForce 6 /
// 7, whose CRTC registers are written directly, Pascal takes its modes through the display engine's core channel
// (EVO): methods in a push buffer, committed by UPDATE, then the supervisor stages, where a driver programs the pixel
// clocks and the outputs (SOR: DVI, HDMI, DisplayPort).
//
// describe() finds the card at boot and writes to the kernel log its video BIOS (the image the system BIOS ran, at
// 0xC0000), the outputs of its DCB table and the timings of the heads the firmware turned on. It changes nothing.
//
// start() takes the display from the video BIOS, gives the display engine its instance memory in VRAM (a hash table
// with the context DMA of VRAM) and starts the core channel, with its push buffer in system memory, as
// gf119_disp_init() and gp102_disp_core_init() do. The GPU raises no interrupt: the steps poll.
//
// scanout() shows a surface of VRAM on the head the video BIOS lit, in the mode (raster, pixel clock, output) the video
// BIOS armed there: the surface is scaled to that raster, keeping its aspect. The supervisor stages that follow an
// UPDATE are acknowledged without touching the clocks or the outputs, which keep their settings.
class NvidiaPascal {
public:
    static bool describe();
    static bool present() { return mmio != nullptr; }
    // 0 once the core channel runs (at once when it already does), or a negative error (-Err::NODEV without a Pascal
    // card or a lit head, -Err::IO when a step times out)
    static int start();
    static bool running() { return started; }
    // Writes to the kernel log the core channel state the display engine runs with (ARM, at 0x640000 + method) and
    // the one its methods assembled (0x660000 + method): the core methods, the SORs and the first two heads.
    static int dumpArmedState();

    // The raster of the lit head: the largest surface scanout() shows unscaled
    static unsigned rasterWidth() { return rasterWidthPixels; }
    static unsigned rasterHeight() { return rasterHeightPixels; }
    // A pointer to VRAM through BAR1, which maps VRAM linearly until a driver gives it a page table; null when the
    // range is outside BAR1
    static volatile uint8_t *vram(uint64_t offset, uint64_t bytes);
    static uint64_t vramPhysical(uint64_t offset) { return aperture + offset; }
    // A8R8G8B8 surface at a VRAM offset (256 byte aligned), pitch a multiple of 256 bytes, at most the raster
    static bool scanout(uint64_t offset, unsigned width, unsigned height, unsigned pitch);

private:
    static volatile uint8_t *mmio;
    static Pci::Location location;
    static unsigned chip;
    // BAR1, the VRAM aperture, found by describe() at boot: sizing a BAR turns the card's memory decoding off for a
    // moment
    static uint64_t aperture;
    static uint64_t apertureSize;
    static bool apertureChecked;
    static bool apertureLinear;
    static unsigned litHead;
    static unsigned rasterWidthPixels;
    static unsigned rasterHeightPixels;
    static bool started;
    static uint32_t *push;
    static uint32_t pushPut;

    // Display engine instance memory (hash table and DMA objects), 64 KB at this VRAM offset
    static constexpr uint64_t DISPLAY_INSTANCE_VRAM = 0x10000000;
    static constexpr unsigned DISPLAY_INSTANCE_BYTES = 64 * 1024;
    static constexpr uint32_t PRAMIN_WINDOW = 0x001700;
    static constexpr uint32_t PRAMIN = 0x700000;
    static constexpr unsigned SORS = 4;
    static constexpr unsigned HEADS = 4;
    static constexpr uint32_t PMC_BOOT_0 = 0x000000;
    static constexpr uint64_t LEGACY_ROM = 0xC0000;
    static constexpr unsigned LEGACY_ROM_BYTES = 128 * 1024;
    // gp102_disp_intr_error(): method, data and error type of a channel's exception, 12 bytes per channel (gf119 has
    // them at 0x6101f0); the core channel is channel 0
    static constexpr uint32_t CORE_ERROR = 0x6111f0;
    static constexpr uint32_t ARMED_STATE = 0x640000;     // what the core channel armed, per head (+0x300)
    static constexpr uint32_t ASSEMBLY_STATE = 0x660000;  // what its methods assembled for the next UPDATE
    static constexpr uint32_t CORE_PUT = 0x640000;
    static constexpr uint32_t CORE_GET = 0x640004;
    static constexpr uint32_t VRAM_HANDLE = 0xf0000001;
    static constexpr uint32_t RAMHT_BITS = 9;
    static constexpr uint32_t DMA_OBJECT_OFFSET = 0x1000;
    static constexpr uint32_t FORMAT_A8R8G8B8 = 0xCF;
    static constexpr unsigned PUSH_BYTES = 4096;
    // cl907d.h methods: UPDATE, SET_NOTIFIER_CONTROL, and the head ones (+ head * 0x300)
    static constexpr uint32_t UPDATE = 0x0080;
    static constexpr uint32_t SET_NOTIFIER_CONTROL = 0x0084;
    static constexpr uint32_t HEAD_SET_CONTROL_OUTPUT_SCALER = 0x0494;
    static constexpr uint32_t HEAD_SET_OFFSET = 0x0460;
    static constexpr uint32_t HEAD_SET_SIZE = 0x0468;
    static constexpr uint32_t HEAD_SET_STORAGE = 0x046c;
    static constexpr uint32_t HEAD_SET_PARAMS = 0x0470;
    static constexpr uint32_t HEAD_SET_CONTEXT_DMAS_ISO = 0x0474;
    static constexpr uint32_t HEAD_SET_VIEWPORT_POINT_IN = 0x04b0;
    static constexpr uint32_t HEAD_SET_VIEWPORT_SIZE_IN = 0x04b8;
    static constexpr uint32_t HEAD_SET_VIEWPORT_SIZE_OUT = 0x04c0;
    static constexpr uint32_t HEAD_SET_VIEWPORT_SIZE_OUT_MIN = 0x04c4;
    static constexpr uint32_t HEAD_SET_VIEWPORT_SIZE_OUT_MAX = 0x04c8;
    // HEAD_SET_STORAGE: PITCH in bits 20:8 (bytes, 256 aligned), MEMORY_LAYOUT PITCH at bit 24 (bit 20 on NV50)
    static constexpr uint32_t STORAGE_PITCH_LAYOUT = 1u << 24;

    static uint32_t rd(uint32_t reg) { return *(volatile uint32_t *)(mmio + reg); }
    static void wr(uint32_t reg, uint32_t value) { *(volatile uint32_t *)(mmio + reg) = value; }
    static void mask(uint32_t reg, uint32_t clear, uint32_t set) { wr(reg, (rd(reg) & ~clear) | set); }
    static bool waitFor(uint32_t reg, uint32_t bits, uint32_t value, unsigned milliseconds);
    static void vramWrite(uint64_t address, uint32_t value);
    static uint32_t vramRead(uint64_t address);
    static void logState(const char *when);
    // Waits until the kernel log left through the serial lines (COM1 at once, the PL2303 adapter through USB) before
    // the next risky step
    static void settle();
    static int takeDisplay();
    static int startCoreChannel();
    static void bindVramContext();
    static bool checkAperture();
    static void reserve(unsigned bytes);
    static void method(uint32_t mthd, uint32_t data);
    static bool kick(unsigned milliseconds);
    static unsigned adoptArmedMode(unsigned head);
    static void supervise(unsigned head, uint64_t offset);
    static void logScanout(unsigned head);

    static bool pascal(unsigned chipset) { return chipset >= 0x130 && chipset <= 0x13B; }
    static const char *chipName(unsigned chipset);
    static void describeBios(const uint8_t *rom, unsigned bytes);
    static void describeHeads();
};

#endif
