#include "programs/nvidia/NvidiaCommand.h"
#include "fs/Err.h"
#include "lib/vitralKernel/Printf.h"
#include "lib/vitralKernel/String.h"
#include "syscalls/Syscalls.h"

int
NvidiaCommand::main(const ArgList &args) {
    const char *what = args.count() > 0 ? args.at(0) : "info";
    if (String::equals(what, "info") && args.count() <= 1) {
        NvidiaInfo info;
        if (!describe(info)) {
            return 1;
        }
        showInfo(info);
        return 0;
    }
    if (String::equals(what, "accel") && args.count() <= 2) {
        if (args.count() == 2) {
            return acceleration(args.at(1));
        }
        NvidiaInfo info;
        if (!describe(info)) {
            return 1;
        }
        showAcceleration(info);
        return 0;
    }
    if (String::equals(what, "test") && args.count() == 1) {
        return test();
    }
    usage();
    return String::equals(what, "help") ? 0 : 1;
}

void
NvidiaCommand::usage() {
    printf64("usage: nvidia [info | accel [on | off] | test | help]\n"
             "  info       the NVIDIA card (RIVA TNT to GeForce 7), its monitor, mode and 2D engine\n"
             "  accel      whether the SVGA modes draw with the 2D engine\n"
             "  accel on   fills and copies of the SVGA modes by the 2D engine\n"
             "  accel off  back to drawing with the CPU in the frame buffer\n"
             "  test       the 2D engine checked against the CPU, and both timed\n");
}

bool
NvidiaCommand::describe(NvidiaInfo &info) {
    int64_t result = Syscalls::nvidia(NvidiaInfo::DESCRIBE, &info);
    if (result == -Err::NODEV) {
        printf64("nvidia: no RIVA TNT / TNT2 or GeForce 6 / 7 drives the screen\n");
        return false;
    }
    if (result < 0) {
        printf64("nvidia: the driver does not answer (error %d)\n", (int)result);
        return false;
    }
    return true;
}

void
NvidiaCommand::showKhz(const char *what, uint32_t khz) {
    if (khz == 0) {
        printf64("  %-18s unknown\n", what);
    } else {
        printf64("  %-18s %u.%03u MHz\n", what, khz / 1000, khz % 1000);
    }
}

void
NvidiaCommand::showInfo(const NvidiaInfo &info) {
    printf64("%s\n", info.chipName);
    printf64("  %-18s %s\n", "family", info.familyName);
    printf64("  %-18s NV%02x, revision %02x (PMC_BOOT_0 %08x)\n", "chipset", (uint32_t)info.chipset,
             (uint32_t)info.revision, info.boot0);
    printf64("  %-18s %02x:%02x.%u, ID %04x:%04x", "PCI", (uint32_t)info.bus, (uint32_t)info.slot,
             (uint32_t)info.function, (uint32_t)info.pciVendor, (uint32_t)info.pciDevice);
    if (info.implementation != info.pciDevice) {
        printf64(" (GPU %04x behind a bridge)", (uint32_t)info.implementation);
    }
    printf64(", subsystem %04x:%04x\n", (uint32_t)info.subsystemVendor, (uint32_t)info.subsystemDevice);
    printf64("  %-18s registers %u MB at 0x%08x, frame buffer %u MB at 0x%08x\n", "apertures",
             (uint32_t)(info.registersBytes >> 20), (uint32_t)info.registersAddress, (uint32_t)(info.frameBytes >> 20),
             (uint32_t)info.frameAddress);
    printf64("  %-18s %s", "video BIOS", info.biosVersion[0] ? info.biosVersion : "version unknown");
    if (info.biosChip) {
        printf64(" (chip 0x%02x)", (uint32_t)info.biosChip);
    }
    printf64("\n");
    printf64("  %-18s %u MB", "video memory", info.memoryBytes >> 20);
    if (info.memoryBusBits) {
        printf64(" on a %u-bit bus", info.memoryBusBits);
    }
    printf64("; modes up to %u KB, 2D engine addresses %u MB\n", info.usableBytes >> 10, info.mapBytes >> 20);
    printf64("  %-18s %08x\n", "straps", info.straps);
    showKhz("crystal", info.crystalKhz);
    showKhz("core clock", info.coreKhz);
    showKhz("memory clock", info.memoryKhz);
    showKhz("max pixel clock", info.maxPixelKhz);
    printf64("  %-18s %s stage, VCO1 %u-%u MHz", "pixel PLL", info.twoStagePll ? "two" : "one",
             info.vco1MinKhz / 1000, info.vco1MaxKhz / 1000);
    if (info.twoStagePll) {
        printf64(", VCO2 %u-%u MHz", info.vco2MinKhz / 1000, info.vco2MaxKhz / 1000);
    }
    printf64("; VPLL %08x", info.vpll);
    if (info.twoStagePll) {
        printf64(" %08x", info.vpllB);
    }
    printf64("\n");
    if (info.monitor) {
        printf64("  %-18s %s %04x \"%s\", %s input, DDC CR%02x\n", "monitor", info.monitorVendor,
                 (uint32_t)info.monitorProduct, info.monitorName, info.monitorAnalog ? "analog" : "digital",
                 (uint32_t)info.ddcPort);
        if (info.preferredWidth) {
            printf64("  %-18s %ux%u at %u Hz, %u kHz pixel clock\n", "preferred mode", (uint32_t)info.preferredWidth,
                     (uint32_t)info.preferredHeight, (uint32_t)info.preferredHz, info.preferredKhz);
        }
    } else {
        printf64("  %-18s no EDID: VESA modes\n", "monitor");
    }
    if (info.width) {
        printf64("  %-18s %ux%u, %u bits per pixel, pitch %u bytes, pixel clock %u kHz\n", "mode",
                 (uint32_t)info.width, (uint32_t)info.height, (uint32_t)info.bytesPerPixel * 8, info.pitch,
                 info.pixelKhz);
    } else {
        printf64("  %-18s VGA (the BIOS state)\n", "mode");
    }
    showAcceleration(info);
}

void
NvidiaCommand::showAcceleration(const NvidiaInfo &info) {
    printf64("  %-18s %s (drawing: %s)\n", "2D acceleration", info.accelerationSelected ? "on" : "off",
             info.primitives);
    if (!info.engineStarted) {
        printf64("  %-18s not started\n", "2D engine");
        return;
    }
    printf64("  %-18s %s, push buffer %u KB at %u KB; GET %u PUT %u, PGRAPH status %08x\n", "2D engine",
             info.engineLockedUp ? "stopped answering" : "running", info.pushBufferBytes >> 10,
             info.pushBufferOffset >> 10, info.fifoGet, info.fifoPut, info.graphStatus);
    printf64("  %-18s %u fills, %u copies, %u kickoffs, %u lockups\n", "2D commands", (uint32_t)info.fills,
             (uint32_t)info.copies, (uint32_t)info.kickoffs, info.lockups);
}

int
NvidiaCommand::acceleration(const char *choice) {
    bool on = String::equals(choice, "on");
    if (!on && !String::equals(choice, "off")) {
        usage();
        return 1;
    }
    int64_t result = Syscalls::nvidia(on ? NvidiaInfo::ACCELERATION_ON : NvidiaInfo::ACCELERATION_OFF);
    if (result == -Err::NODEV) {
        printf64("nvidia: no RIVA TNT / TNT2 or GeForce 6 / 7 drives the screen\n");
        return 1;
    }
    if (result < 0) {
        printf64("nvidia: the 2D engine does not start: drawing stays with the CPU (see dmesg)\n");
        return 1;
    }
    printf64("nvidia: 2D acceleration %s\n", on ? "on: fills and copies by the 2D engine" : "off: CPU drawing");
    return 0;
}

void
NvidiaCommand::showRate(const char *what, uint32_t engine, uint32_t cpu) {
    printf64("  %-6s 2D engine %5u.%u Mpixel/s, CPU %5u.%u Mpixel/s", what, engine / 10, engine % 10, cpu / 10,
             cpu % 10);
    if (cpu) {
        printf64(" (x%u)", engine / cpu);
    }
    printf64("\n");
}

int
NvidiaCommand::test() {
    NvidiaInfo info;
    int64_t result = Syscalls::nvidia(NvidiaInfo::TEST, &info);
    if (result == -Err::NODEV) {
        printf64("nvidia: no RIVA TNT / TNT2 or GeForce 6 / 7 drives the screen\n");
        return 1;
    }
    if (result < 0) {
        printf64("nvidia: the driver does not answer (error %d)\n", (int)result);
        return 1;
    }
    const NvidiaInfo::Test &t = info.test;
    printf64("%s: 2D engine self test %s", info.chipName, t.passed ? "passed" : "FAILED");
    if (t.failure[0]) {
        printf64(": %s", t.failure);
    }
    printf64("\n");
    if (t.checkedPixels) {
        printf64("  fill, copy and overlapping copy: %u pixels checked, %u wrong\n", t.checkedPixels, t.wrongPixels);
    }
    if (t.wrongPixels) {
        printf64("  first at (%u, %u): %06x instead of %06x\n", t.firstWrongX, t.firstWrongY, t.found, t.expected);
    }
    if (t.engineFill || t.cpuFill) {
        showRate("fill", t.engineFill, t.cpuFill);
        showRate("copy", t.engineCopy, t.cpuCopy);
    }
    return t.passed ? 0 : 1;
}
