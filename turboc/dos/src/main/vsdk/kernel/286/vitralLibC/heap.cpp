#include "vitralLibC/crt.h"
#ifdef __TURBOC__
#include <dos.h>
#endif

struct Crt286Block {
    unsigned bytes;
    unsigned free;
    Crt286Block CRT286_FAR* next; /* Free-list link; zero while allocated. */
};
static Crt286Block CRT286_FAR* arenas[240];
static Crt286Block CRT286_FAR* freeLists[240];
static unsigned freeBytes[240];
static unsigned arenaBytes[240];
static unsigned arenaCount;

extern "C" void crt286Initialize()
{
    /* Call once, before constructors. Resetting a live heap invalidates it. */
    arenaCount = 0;
    crt286ResetCallbacks();
}

extern "C" int crt286AddArena(void CRT286_FAR* memory, unsigned bytes)
{
    Crt286Block CRT286_FAR* block;
    if (!memory || arenaCount == 240 || bytes > 65534U ||
        bytes < sizeof(Crt286Block) + 2 ||
        ((unsigned long)memory & (sizeof(void*) - 1U))) return 0;
#ifdef __TURBOC__
    if ((unsigned long)FP_OFF(memory) + bytes > 65536UL) return 0;
#endif
    block = (Crt286Block CRT286_FAR*)memory;
    block->bytes = (unsigned)(bytes - sizeof(Crt286Block));
    block->free = 1;
    block->next = 0;
    /* Count complete free regions, including their block headers. */
    freeBytes[arenaCount] = bytes;
    arenaBytes[arenaCount] = bytes;
    freeLists[arenaCount] = block;
    arenas[arenaCount++] = block;
    return 1;
}

extern "C" void CRT286_FAR* crt286Allocate(size_t bytes)
{
    unsigned i;
    Crt286Block CRT286_FAR* block;
    Crt286Block CRT286_FAR* tail;
    Crt286Block CRT286_FAR* previous;
    if (bytes == 0) bytes = 1;
    if (bytes > 65534U - sizeof(Crt286Block) - sizeof(void*)) return 0;
    bytes = (bytes + sizeof(void*) - 1U) & ~(size_t)(sizeof(void*) - 1U);
    for (i = 0; i < arenaCount; ++i) {
        if (freeBytes[i] < bytes + sizeof(Crt286Block)) continue;
        previous = 0;
        for (block = freeLists[i]; block; previous = block, block = block->next) {
            if (block->bytes < bytes) continue;
            tail = block->next;
            if (block->bytes - bytes >= sizeof(Crt286Block) + 2) {
                tail = (Crt286Block CRT286_FAR*)
                    ((unsigned char CRT286_FAR*)(block + 1) + bytes);
                tail->bytes = (unsigned)(block->bytes - bytes - sizeof(Crt286Block));
                tail->free = 1;
                tail->next = block->next;
                block->bytes = (unsigned)bytes;
            }
            if (previous) previous->next = tail;
            else freeLists[i] = tail;
            freeBytes[i] -= (unsigned)(block->bytes + sizeof(Crt286Block));
            block->free = 0;
            block->next = 0;
            return block + 1;
        }
    }
    return 0;
}

extern "C" void crt286Release(void CRT286_FAR* memory)
{
    unsigned i;
    Crt286Block CRT286_FAR* block;
    Crt286Block CRT286_FAR* previous;
    Crt286Block CRT286_FAR* next;
    if (!memory) return;
    for (i = 0; i < arenaCount; ++i) {
#ifdef __TURBOC__
        /* Ordinary far pointers retain the arena's selector. */
        if (FP_SEG(memory) != FP_SEG(arenas[i])) continue;
        unsigned long offset = (unsigned long)FP_OFF(memory) - FP_OFF(arenas[i]);
#else
        /* Host probes use flat pointers; size_t holds addresses there. */
        size_t offset = (size_t)memory - (size_t)arenas[i];
#endif
        if (offset < sizeof(Crt286Block) || offset >= arenaBytes[i] ||
            (offset & (sizeof(void*) - 1U))) continue;
        block = (Crt286Block CRT286_FAR*)memory - 1;
        if (block->free || block->next || block->bytes > arenaBytes[i] - offset) return;
        /* Only walk free blocks, in address order within this one arena.
         * Its selector is shared, so offset ordering is sufficient here. */
        previous = 0;
        next = freeLists[i];
        while (next && (unsigned char CRT286_FAR*)next <
                       (unsigned char CRT286_FAR*)block) {
            previous = next;
            next = next->next;
        }
        freeBytes[i] += (unsigned)(block->bytes + sizeof(Crt286Block));
        block->free = 1;
        block->next = next;
        if (previous) previous->next = block;
        else freeLists[i] = block;
        if (next && (unsigned char CRT286_FAR*)(block + 1) + block->bytes ==
                    (unsigned char CRT286_FAR*)next) {
            block->bytes = (unsigned)(block->bytes + sizeof(Crt286Block) + next->bytes);
            block->next = next->next;
        }
        if (previous && (unsigned char CRT286_FAR*)(previous + 1) + previous->bytes ==
                        (unsigned char CRT286_FAR*)block) {
            previous->bytes = (unsigned)(previous->bytes + sizeof(Crt286Block) + block->bytes);
            previous->next = block->next;
        }
        return;
    }
}
