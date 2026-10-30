#ifndef __TRAP_FRAME__
#define __TRAP_FRAME__

// The registers an interrupt saves depend on the CPU mode the kernel is built for. Both classes offer the same
// interface (number(), argument(), setResult()...), so the code that uses a TrapFrame is the same in both builds.
#if defined(__x86_64__)
#include "kernel/x86_64/TrapFrame.h"
#elif defined(__i386__)
#include "kernel/i386/TrapFrame.h"
#else
#error "unsupported architecture"
#endif

#endif
