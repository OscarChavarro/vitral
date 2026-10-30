#include "drivers/Protected286Keyboard.h"
#include "vitralLibC/Protected286Crt.h"
#include "fs/Protected286Fat.h"
#include "kernel/Protected286Memory.h"
#include "drivers/Protected286Ports.h"
#include "kernel/Protected286Runtime.h"
#include "svgalib/Protected286VgaText.h"
#include "vitralLibC/crt.h"
#include <dos.h>

static Protected286Fat* protected286Fat;
extern "C" void crt286InitializeStreams();
extern "C" void crt286InitializeCtype();
extern "C" void crt286InitializeDirectories();

void
Protected286Runtime::initialize()
{
    Protected286VgaText::initialize();
    Protected286VgaText::writeAt(0, 14, "286 [14] Initializing protected memory.\r\n");
    Protected286Memory::initialize();
    Protected286VgaText::write("286 [15] Initializing keyboard and CRT.\r\n");
    Protected286Keyboard::initialize();
    Protected286Crt::initialize();
    crt286InitializeStreams();
    crt286InitializeCtype();
    crt286InitializeDirectories();
    crt286Initialize();
    Protected286Crt::writeLine("286 [16] Registering high-memory heap arenas.");
    /* Reserve up to 4 MiB for C++ objects and BREP modeling. Framebuffers use their
     * own selectors and retain the rest of the reserved high-memory pool. */
    for (unsigned i = 0; i < 64; ++i) {
        Protected286Word selector = Protected286Memory::allocateSegment();
        if (!selector) break;
        if (!crt286AddArena(MK_FP(selector, 0), 65532U)) {
            Protected286Memory::releaseSegment(selector);
            break;
        }
    }
    Protected286Crt::writeLine("286 [17] Allocating FAT backend.");
    protected286Fat = new Protected286Fat();
    Protected286Crt::writeLine("286 [18] Mounting IDE/FAT root.");
    if (protected286Fat && protected286Fat->mount()) {
        Protected286Crt::installFileSystem(protected286Fat);
        Protected286Crt::writeLine("IDE/FAT C: backend ready.");
    }
    else {
        Protected286Crt::writeLine("IDE/FAT C: backend unavailable.");
    }
}

void
Protected286Runtime::acknowledgeMasterPic()
{
    Protected286Ports::outByte(0x20, 0x20);
}

void
Protected286Runtime::reboot267System()
{
    while (Protected286Ports::inByte(0x64) & 2) {
    }
    Protected286Ports::outByte(0x64, 0xFE);
    for (;;) {
        asm hlt;
    }
}
