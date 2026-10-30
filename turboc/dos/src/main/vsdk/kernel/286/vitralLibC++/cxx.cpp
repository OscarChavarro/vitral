/* Compile only for the freestanding Turbo C++ large-model target. */
#include "vitralLibC/crt.h"
#include <new.h>
#include "vitralLibC/Protected286Crt.h"
#include "kernel/Protected286Runtime.h"

/* Filled by the real-mode loader from DOS, before copying the image. */
unsigned _psp = 0;
unsigned _version = 0;
/* MATHL dispatch level: use the 287-compatible path even on a newer FPU.
 * The assembly bootstrap verifies hardware presence before entering PE. */
int _8087 = 2;
/* No signal handlers are installed. Borland FPERR explicitly handles a
 * null registry by printing the error and taking our abort path. */
void far* __SignalPtr = 0;

void* operator new(size_t bytes) { return crt286Allocate(bytes); }
void operator delete(void* memory) { crt286Release(memory); }

extern "C" void* malloc(size_t bytes) { return crt286Allocate(bytes); }
extern "C" void free(void* memory) { crt286Release(memory); }

extern "C" void abort()
{
    Protected286Crt::writeLine("Fatal C++ runtime error. Press a key to reset.");
    Protected286Crt::getCharacter();
    Protected286Runtime::reboot267System();
    for (;;) {}
}

extern "C" void exit(int status)
{
    crt286Finalize();
    Protected286Crt::write("Application exited with status ");
    Protected286Crt::writeUnsigned((unsigned long)status);
    Protected286Crt::writeLine(". Press a key to reset.");
    Protected286Crt::getCharacter();
    Protected286Runtime::reboot267System();
    for (;;) {}
}

extern "C" void _fpure_error_() { abort(); }

/* Turbo C++ 3.0 uses the scalar allocation operators for arrays too.
 * Do not introduce GCC ABI symbols (__cxa_*, sized delete) here. */
extern "C" int atexit(void (*function)())
{
    return crt286AtExit(function);
}
