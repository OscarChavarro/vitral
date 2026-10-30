#include "kernel/ArgList.h"
#include "kernel/Program.h"
#include "lib/vitralKernel/Memory.h"
#include "lib/vitralKernel/String.h"
#include "kernel/ProgramRegistry.h"

bool
ArgList::add(const char *arg) {
    size_t len = String::length(arg) + 1;
    if (n >= MAX_ARGS || used + len > BUFFER_SIZE) {
        return false;
    }
    Memory::copy(buffer + used, arg, len);
    offset[n++] = (uint16_t)used;
    used += (unsigned)len;
    return true;
}
