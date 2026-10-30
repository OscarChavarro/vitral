#ifndef __DMESG_COMMAND__
#define __DMESG_COMMAND__
#include <stddef.h>
#include <stdint.h>
#include "kernel/ArgList.h"

class DmesgCommand {
public:
    static int main(const ArgList &args);

private:
    static constexpr uint8_t NORMAL = 0x07;
    static constexpr uint8_t PROMPT = 0x70;

    static unsigned rowsOf(size_t length);
};

#endif
