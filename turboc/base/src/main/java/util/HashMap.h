#ifndef __HASH_MAP__
#define __HASH_MAP__

#include <stddef.h>
#include <string.h>

#include "java/lang/Object.h"
#include "java/lang/String.h"
#include "java/util/HashMapEntry.h"
template <class K, class V>
class HashMap: public Object {
  private:
    typedef HashMapEntry<K, V> Entry;

    Entry **buckets;
    long bucketCount;
    long elementCount;
    float maxLoadFactor;

    void initialize(long initialBucketCount);
    void rehash(long newBucketCount);
    long bucketIndexFor(const K &key) const;
    static size_t hashFNV1a(const unsigned char *bytes, size_t length);

    template <class T>
    static size_t hashKeyValue(const T &value);

    static size_t hashKeyValue(const String &value);

    template <class T>
    static size_t hashKeyValue(T *const &value);

    template <class T>
    static size_t hashKeyValue(const T *const &value);

  public:
    HashMap();
    explicit HashMap(long initialCapacity);
    HashMap(const HashMap &other);
    ~HashMap();

    HashMap &operator=(const HashMap &other);

    void clear();
    long size() const;
    bool isEmpty() const;

    bool containsKey(const K &key) const;
    V *get(const K &key);
    const V *get(const K &key) const;
    bool tryGet(const K &key, V *valueOut) const;
    V getOrDefault(const K &key, const V &defaultValue) const;
    bool put(const K &key, const V &value);
    bool remove(const K &key);
};

#include "java/util/HashMap.txx"

#endif
