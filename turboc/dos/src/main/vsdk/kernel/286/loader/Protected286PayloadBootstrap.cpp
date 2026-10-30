// Separate real-mode executable: stream the selected payload into extended memory.
// Both links MUST put the identical ModeSwitch286 object first. Its high
// protectedEntry offset is therefore identical in the launcher and payload.
#define TURBO_C_286_EXTERNAL_PAYLOAD
#include "boot/Protected286Loader.cpp"

unsigned _psp = 0;
unsigned _version = 0;

// Satisfies the launcher's unused protected call relocation. The actual
// protectedEntry runs from the payload and calls its relocated C++ kernel.
extern "C" void protected286KernelMain()
{
    for (;;) { asm hlt; }
}
