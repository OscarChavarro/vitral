#ifndef __HASH_MAP_ITERATION_ORDER__
#define __HASH_MAP_ITERATION_ORDER__

#include "java/util/ArrayList.h"

namespace java {

/**
Iteration order of a Java `java.util.HashMap<Integer, V>` built with the
default constructor and only `put` calls (no removals), so that C++ ports
that iterate such a map (`keySet`, `values`, `entrySet`) visit its entries
in the same order as Java does. `java::HashMap` hashes with FNV-1a and has
no iterators, so the ports keep the keys in insertion order and ask this
class for the Java order.

Java's table starts with 16 buckets and doubles while `size > 0.75 *
capacity`; an `Integer` key goes to bucket `(h ^ (h >>> 16)) & (capacity -
1)` with `h = key`, and a bucket keeps its entries in insertion order (a
resize splits a bucket preserving the relative order). Buckets turned into
trees (more than 8 colliding keys in a table of 64 or more buckets) are
not emulated.
*/
class HashMapIterationOrder {
public:
    /**
    @param keysInInsertionOrder distinct keys, in the order they were first
    put in the map
    @return the same keys in the order a Java `HashMap` iterates them
    */
    static ArrayList<int> integerKeys(const ArrayList<int>& keysInInsertionOrder)
    {
        long int n = keysInInsertionOrder.size();
        long int capacity = 16;
        while ( (double)n > 0.75 * (double)capacity ) {
            capacity *= 2;
        }
        // Stable counting sort of the keys by bucket
        ArrayList<int> start;
        long int bucket;
        long int i;
        for ( bucket = 0; bucket <= capacity; bucket++ ) {
            start.add(0);
        }
        for ( i = 0; i < n; i++ ) {
            start[bucketOf(keysInInsertionOrder.get(i), capacity) + 1]++;
        }
        for ( bucket = 1; bucket <= capacity; bucket++ ) {
            start[bucket] += start[bucket - 1];
        }
        ArrayList<int> out;
        for ( i = 0; i < n; i++ ) {
            out.add(0);
        }
        for ( i = 0; i < n; i++ ) {
            int key = keysInInsertionOrder.get(i);
            out[(long int)start[bucketOf(key, capacity)]++] = key;
        }
        return out;
    }

private:
    static long int bucketOf(int key, long int capacity)
    {
        unsigned int h = (unsigned int)key;
        h = h ^ (h >> 16);
        return (long int)(h & (unsigned int)(capacity - 1));
    }

    HashMapIterationOrder() {}
};

}

#endif
