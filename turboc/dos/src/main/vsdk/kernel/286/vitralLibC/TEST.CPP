#include "vitralLibC/CRT.H"
#include <assert.h>

union Arena { long double alignment; void* pointer; unsigned char bytes[4096]; };
static Arena a, b;
static int sequence;
static void first() { sequence = sequence * 10 + 1; }
static void second() { sequence = sequence * 10 + 2; }

int main()
{
    void* p;
    void* q;
    void* r;
    void* slots[512];
    unsigned i, count;
    Crt286Callback constructors[2] = { first, second };
    crt286Initialize();
    assert(crt286Allocate(1) == 0);
    assert(!crt286AddArena(0, 100));
    assert(!crt286AddArena(a.bytes, 1));
    assert(crt286AddArena(a.bytes, sizeof(a.bytes)));
    p = crt286Allocate(100);
    q = crt286Allocate(200);
    assert(p && q && p != q);
    ((unsigned char*)p)[99] = 42;
    ((unsigned char*)q)[199] = 43;
    assert(((unsigned char*)p)[99] == 42);
    crt286Release(p);
    crt286Release(q);
    /* Requires coalescing both freed blocks and the unused tail. */
    r = crt286Allocate(3900);
    assert(r);
    assert(crt286Allocate(65535U) == 0);
    crt286Release(r);
    count = 0;
    while (count < 512 && (slots[count] = crt286Allocate(32)) != 0) ++count;
    assert(count > 0 && count < 512);
    assert(crt286AddArena(b.bytes, sizeof(b.bytes)));
    p = crt286Allocate(3000);
    assert(p);
    for (i = 0; i < count; ++i) crt286Release(slots[i]);
    crt286Release(p);
    crt286Release(0);
    /* Repeatedly fill both arenas, fragment them, then merge free neighbors
     * in different orders. All capacity must be reusable after each pass. */
    for (unsigned pass = 0; pass < 8; ++pass) {
        count = 0;
        while (count < 512 && (slots[count] = crt286Allocate(32 + (count % 7) * 4)) != 0)
            ++count;
        assert(count > 2 && count < 512);
        for (i = 0; i < count; i += 2) crt286Release(slots[i]);
        for (i = count; i > 0; --i)
            if ((i - 1) % 2) crt286Release(slots[i - 1]);
        p = crt286Allocate(3900);
        q = crt286Allocate(3900);
        assert(p && q && p != q);
        crt286Release(q);
        crt286Release(p);
    }
    assert(crt286Allocate(0));
    crt286Construct(constructors, 2);
    assert(sequence == 12);
    sequence = 0;
    assert(crt286AtExit(first) == 0);
    assert(crt286AtExit(second) == 0);
    crt286Finalize();
    assert(sequence == 21);
    crt286Finalize();
    assert(sequence == 21);
    assert(crt286AtExit(0) == -1);
    for (i = 0; i < 64; ++i) assert(crt286AtExit(first) == 0);
    assert(crt286AtExit(second) == -1);
    return 0;
}
