#ifndef __ARG_LIST__
#define __ARG_LIST__
#include <stdint.h>

class ArgList {
public:
    static constexpr unsigned MAX_ARGS = 8;
    static constexpr unsigned BUFFER_SIZE = 256;

    ArgList() : n(0), used(0) {}
    bool add(const char *arg);
    unsigned count() const { return n; }
    const char *at(unsigned i) const { return i < n ? buffer + offset[i] : nullptr; }

private:
    char buffer[BUFFER_SIZE];
    uint16_t offset[MAX_ARGS];
    unsigned n;
    unsigned used;
};

#endif
