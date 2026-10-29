/* Turbo C++ large-model vector ABI (far objects, far cdecl callbacks).
 * The four-byte count cookie precedes the returned element pointer. */
#include "CRT.H"
#include <stdarg.h>
#include <stdlib.h>

typedef void (far *Constructor)(void far*);
typedef void (far *Destructor)(void far*, int);

void far* _vector_new_(void far* memory, unsigned size, unsigned long count,
                       unsigned flags, ...)
{
    va_list args;
    Constructor constructor;
    unsigned prefix = (flags & 0x10 ? 4 : 0) + (flags & 0x20 ? 2 : 0);
    unsigned long i;
    char far* elements = (char far*)memory;
    if ((flags & 7) != 5 || (flags & ~0x3FU) ||
        ((flags & 0x20) && !(flags & 0x10))) abort();
    va_start(args, flags);
    constructor = va_arg(args, Constructor);
    va_end(args);
    if (!size || count > (65500UL - prefix) / size) return 0;
    if (!elements) {
        char far* allocation = (char far*)crt286Allocate((size_t)(count * size + prefix));
        if (!allocation) return 0;
        elements = allocation;
        if (flags & 0x20) { *(unsigned far*)elements = size; elements += 2; }
        if (flags & 0x10) { *(unsigned long far*)elements = count; elements += 4; }
    }
    if (constructor) for (i = 0; i < count; ++i) (*constructor)(elements + (unsigned)(i * size));
    return elements;
}

void _vector_delete_(void far* memory, unsigned size, unsigned long count,
                      unsigned flags, ...)
{
    va_list args;
    Destructor destructor;
    char far* elements = (char far*)memory;
    unsigned prefix = (flags & 0x10 ? 4 : 0) + (flags & 0x20 ? 2 : 0);
    if (!memory) return;
    if ((flags & 7) != 5 || (flags & ~0x3FU) ||
        ((flags & 0x20) && !(flags & 0x10))) abort();
    va_start(args, flags);
    destructor = va_arg(args, Destructor);
    va_end(args);
    if (flags & 0x10) count = *(unsigned long far*)(elements - 4);
    if (flags & 0x20) size = *(unsigned far*)(elements - 6);
    if (!size || count > (65500UL - prefix) / size) abort();
    if (destructor) while (count) {
        --count;
        (*destructor)(elements + (unsigned)(count * size), 2);
    }
    if (flags & 8) crt286Release(elements - prefix);
}
