#include "drivers/block/BlockDevice.h"
#include "drivers/char/uart/Uart.h"
#include "drivers/hid/MouseDevice.h"
#include "drivers/hid/psmouse/PsMouse.h"
#include "drivers/keyboard/Keyboard.h"
#include "drivers/video/svga/Svga.h"
#include "drivers/video/virtualConsole/VirtualConsole.h"
#include "fs/Vfs.h"
#include "kernel/Clock.h"
#include "kernel/Fpu.h"
#include "kernel/Gdt.h"
#include "kernel/Idle.h"
#include "kernel/Interrupts.h"
#include "kernel/Kernel.h"
#include "kernel/Scheduler.h"
#include "kernel/WallClock.h"
#include "syscalls/Syscalls.h"
#include "boot/BootInfo.h"
#include "kernel/KernelHeap.h"
#include "kernel/Paging.h"
#include "kernel/PhysicalMemory.h"
#include "kernel/Pic.h"
#include "kernel/Smp.h"
#include "lib/vitralKernel/Calendar.h"
#include "kernel/KernelLog.h"
#include "lib/vitralStdC++/runtime.h"
#include "drivers/net/e1000/E1000.h"
#include "drivers/net/rtl8169/Rtl8169.h"
#include "drivers/net/tg3/Tg3.h"
#include "net/Net.h"
#include "lib/vitralKernel/DateTime.h"
#include "drivers/block/BlockDevices.h"

// Entered from the image's entry code (boot/entry64.S or boot/i386/entry32kernel.S) once .bss is clear. The same code
// runs in both builds: what depends on the CPU mode is behind Gdt, Interrupts, Fpu, Paging and TrapFrame.
[[noreturn]] void
kernelMain() {
    VirtualConsole::init();
    Fpu::enable();
    Uart::initCom1();
    kprintf(sizeof(void *) == 8 ? "LONG MODE 64-BIT\n" : "PROTECTED MODE 32-BIT (i386 kernel)\n");
    WallClock::recordStart();
    Smp::detect();
    Smp::report();

    const BootInfo &info = BootInfo::get();
    PhysicalMemory::init(info);
    Paging::init(info);
    KernelHeap::init();
    Runtime::init();
    kprintf("Memory: %u MB RAM (top 0x%x%08x), %u MB free, map %u GB, heap %u KB\n",
             (uint32_t)(PhysicalMemory::totalFrames() * 4 / 1024),
             (uint32_t)(PhysicalMemory::highestAddress() >> 32), (uint32_t)PhysicalMemory::highestAddress(),
             (uint32_t)(PhysicalMemory::freeFrameCount() * 4 / 1024), (uint32_t)(Paging::mappedBytes() >> 30),
             (uint32_t)(KernelHeap::sizeBytes() / 1024));

    Gdt::load();
    Interrupts::init();
    Pic::init();
    Clock::init();
    Keyboard::init();
    MouseDevice::init();
    PsMouse::init();
    Svga::init();
    if (Svga::supportsFramebufferText()) {
        VirtualConsole::setTextMode(VgaMode::Id::FramebufferText);
    }
    kprintf("IDT ready: 256 vectors, PIC remapped to 0x20, unhandled ones ignored\n");
    Interrupts::enable();

    while (Clock::ticks() < 19) {
        asm volatile("hlt");
    }
    kprintf("Timer running: %u ticks, %u ms since boot\n", (uint32_t)Clock::ticks(),
             (uint32_t)Clock::uptimeMs());
    kprintf("Idle operation: %s\n", Idle::name());
    WallClock::init();
    DateTime now;
    Calendar::fromUnix(WallClock::seconds(), now);
    kprintf("Real time clock: %s %s %2u %02u:%02u:%02u UTC %d%s\n", Calendar::weekdayName(now.weekday),
             Calendar::monthName(now.month), now.day, now.hour, now.minute, now.second, now.year,
             WallClock::fromRtc() ? "" : " (RTC not usable)");
    BlockDevices::init();
    Vfs::init();
    kprintf("Block devices: /dev/loop0 (%u KB), mounted on /media/ramdisk\n",
             (uint32_t)(BlockDevices::find("loop0")->sizeBytes() / 1024));
    Net::init();
    if (!E1000::init() && !Rtl8169::init() && !Tg3::init()) {
        kprintf("Network: no supported network card found\n");
    }
    Scheduler::init(Smp::logicalCount());
    Syscalls::init();
    Kernel::startInit("init");
    if (Smp::isSmp()) {
        Smp::startProcessors();
        Scheduler::useLapicTimers();
    }
    Scheduler::start();
}
