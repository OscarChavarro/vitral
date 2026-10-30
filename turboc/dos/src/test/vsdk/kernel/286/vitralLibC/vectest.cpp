/* Hosted probe: allocation is supplied by DOS, vector mechanics by VECTOR. */
#include "vitralLibC/crt.h"
#include <stdlib.h>
#include <assert.h>
static int constructed, destroyed, released;
static int order[4];
extern "C" void far* crt286Allocate(size_t n) { return malloc(n); }
extern "C" void crt286Release(void far* p) { ++released; free(p); }
class Item {
    int id;
public:
    Item() { id = constructed++; }
    ~Item() { order[destroyed++] = id; }
};
int main()
{
    Item* items = new Item[4];
    assert(items && constructed == 4);
    delete[] items;
    assert(destroyed == 4 && released == 1);
    for (int i = 0; i < 4; ++i) assert(order[i] == 3 - i);
    return 0;
}
