#ifndef __IDT_GATE__
#define __IDT_GATE__

// The layout of an IDT entry depends on the CPU mode the kernel is built for; both define IdtGate::interrupt().
#if defined(__x86_64__)
#include "kernel/x86_64/IdtGate.h"
#elif defined(__i386__)
#include "kernel/i386/IdtGate.h"
#else
#error "unsupported architecture"
#endif

#endif
