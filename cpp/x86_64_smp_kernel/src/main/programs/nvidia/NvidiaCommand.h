#ifndef __NVIDIA_COMMAND__
#define __NVIDIA_COMMAND__
#include <stdint.h>
#include "drivers/video/nvidiaPreTesla/NvidiaInfo.h"
#include "kernel/ArgList.h"

// nvidia [info | accel [on | off] | test | help]: the pre-Tesla NVIDIA card (RIVA TNT to GeForce 7) and its 2D
// acceleration. "info" (the default) describes the chip, its memory, clocks and BIOS, the monitor, the mode on the
// screen and the 2D engine; "accel on" makes the SVGA modes draw their fills and copies with the 2D engine, "accel
// off" puts the frame buffer (CPU) drawing back; "test" checks the 2D engine against the CPU in video memory the screen
// does not show and compares their speed.
class NvidiaCommand {
public:
    static int main(const ArgList &args);

private:
    static void usage();
    static bool describe(NvidiaInfo &info);
    static void showInfo(const NvidiaInfo &info);
    static void showAcceleration(const NvidiaInfo &info);
    static int acceleration(const char *choice);
    static int test();
    static void showRate(const char *what, uint32_t engine, uint32_t cpu);
    static void showKhz(const char *what, uint32_t khz);
};

#endif
