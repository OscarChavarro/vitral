#ifndef __KERNEL_PIC__
#define __KERNEL_PIC__
#include <stdint.h>

class Pic {
public:
    static void init();
    static void mask(unsigned irq);
    static void unmask(unsigned irq);
    static void eoi(unsigned irq);
    static void disable();

private:
    static uint16_t readIsr();
};

#endif
