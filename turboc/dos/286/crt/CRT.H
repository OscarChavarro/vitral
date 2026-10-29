#ifndef VITRAL_286_CRT_H
#define VITRAL_286_CRT_H

#include <stddef.h>

#ifdef __TURBOC__
#define CRT286_FAR far
#else
#define CRT286_FAR
#endif

/* Arenas must be writable, disjoint and confined to a single selector.
 * No routine here creates descriptors or invokes DOS/BIOS/XMS. */
typedef void (CRT286_FAR *Crt286Callback)();
extern "C" {
void crt286Initialize();
int crt286AddArena(void CRT286_FAR* memory, unsigned bytes);
void CRT286_FAR* crt286Allocate(size_t bytes);
void crt286Release(void CRT286_FAR* memory);
int crt286AtExit(Crt286Callback function);
void crt286Finalize();
void crt286Construct(const Crt286Callback* first, unsigned count);
void crt286ResetCallbacks();
int crt286RunInitTable(unsigned long physical, unsigned bytes, int reverse);
}

#endif
