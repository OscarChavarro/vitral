#ifndef __KERNEL__
#define __KERNEL__

class Process;

class Kernel {
public:
    static Process *startInit(const char *name);
};

#endif
