/* Hosted container regression; synthetic far pointers are never dereferenced. */
#include <assert.h>
#ifdef __TURBOC__
#include <dos.h>
#endif
#include "java/util/TreeSet.h"
#include "java/util/TreeMap.h"

class ValueKey {
public:
    int value;
    ValueKey(int v = 0) : value(v) {}
    int operator<(const ValueKey& other) const { return value < other.value; }
    int operator==(const ValueKey& other) const { return value == other.value; }
};

int main()
{
#ifdef __TURBOC__
    int* a = (int*)MK_FP(0x1000, 0x1234);
    int* b = (int*)MK_FP(0x2000, 0x1234);
    int* absent = (int*)MK_FP(0x3000, 0x1234);
#else
    int storage[3];
    int* a = &storage[0];
    int* b = &storage[1];
    int* absent = &storage[2];
#endif
    TreeSet<int*> nodes;
    assert(nodes.add(b));
    assert(nodes.add(a));
    assert(!nodes.add(a));
    assert(nodes.size() == 2 && nodes.contains(a) && nodes.contains(b));
    assert(!nodes.contains(absent));
    TreeSet<int*> copy(nodes);
    assert(copy.equals(nodes));
    assert(nodes.remove(a) && !nodes.contains(a) && nodes.contains(b));
    assert(!copy.equals(nodes));
    TreeMap<int*, int> indexes;
    assert(indexes.put(a, 11) && indexes.put(b, 22));
    assert(indexes.size() == 2);
    assert(*indexes.get(a) == 11 && *indexes.get(b) == 22);
    assert(indexes.get(absent) == 0);
    assert(!indexes.put(a, 33) && *indexes.get(b) == 22);
    assert(indexes.remove(a) && !indexes.containsKey(a));
    assert(indexes.containsKey(b) && *indexes.get(b) == 22);
    NaturalOrderComparator<int*> order;
    assert(order.compare(a, b) != 0);
    assert(order.compare(a, b) == -order.compare(b, a));
    assert(naturalOrderLess(1, 2) && !naturalOrderLess(2, 1));
    TreeMap<ValueKey, int> values;
    const ValueKey first(1);
    ValueKey second(2);
    assert(values.put(first, 10) && values.put(second, 20));
    assert(*values.get(first) == 10 && *values.get(second) == 20);
    return 0;
}
