#ifndef __PANIC__
#define __PANIC__
#include "kernel/TrapFrame.h"

class Panic {
public:
    [[noreturn]] static void halt(const char *message);
    [[noreturn]] static void exception(const TrapFrame &frame);

private:
    // The registers of the CPU mode the kernel is built for (kernel/<arch>/PanicRegisters.cpp).
    static void printRegisters(const TrapFrame &frame);
};

#endif
