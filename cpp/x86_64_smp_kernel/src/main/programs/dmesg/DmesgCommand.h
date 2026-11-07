#ifndef __DMESG_COMMAND__
#define __DMESG_COMMAND__
#include <stddef.h>
#include <stdint.h>
#include "kernel/ArgList.h"

class DmesgCommand {
public:
    static int main(const ArgList &args);

private:
    static constexpr int KEY_ENTER = '\n';
    static constexpr int KEY_ESC = 27;
    static constexpr uint32_t ESCAPE_WAIT_MS = 50;

    static unsigned rowsOf(size_t length, unsigned columns);
    static void print(const char *text);
    static int waitKey();
};

#endif
