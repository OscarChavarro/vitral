#include "boot/A20.h"
#include "boot/BootInfo.h"
#include "boot/E820.h"
#include "boot/KernelLoader.h"
#include "drivers/video/vga/Vga16.h"
#include "lib/vitralKernel/Printf.h"

[[noreturn]] void
enter_protected();

[[noreturn]] static void
fail(const char *why) {
    printf16("BOOT ERROR: %s\n", why);
    for (;;) {
        asm volatile("cli; hlt");
    }
}

void
kmain() {
    Vga16::clear(0x07);
    printf16("REAL MODE 16-BIT\n");

    BootInfo &info = BootInfo::get();
    info.magic = BootInfo::MAGIC;
    info.frameBufferAddress = 0;
    info.frameBufferPitch = 0;
    info.frameBufferWidth = 0;
    info.frameBufferHeight = 0;
    info.frameBufferBytes = 0;
    info.frameBufferType = 0;
    info.frameBufferRedPosition = 0;
    info.frameBufferRedSize = 0;
    info.frameBufferGreenPosition = 0;
    info.frameBufferGreenSize = 0;
    info.frameBufferBluePosition = 0;
    info.frameBufferBlueSize = 0;

    if (!A20::enable()) {
        fail("A20 gate cannot be enabled.");
    }
    info.a20Enabled = 1;
    printf16("A20 gate enabled\n");

    E820::collect(info);
    if (info.e820Count == 0) {
        fail("the BIOS gave no memory map (INT 15h E820).");
    }
    printf16("Memory map: %u regions\n", info.e820Count);

    printf16("Loading kernel ");
    if (!KernelLoader::load(info)) {
        fail("cannot load the kernel image from the disk.");
    }
    printf16(" %u bytes at 0x%x\n", info.kernelImageBytes, info.kernelBase);

    enter_protected();
}
