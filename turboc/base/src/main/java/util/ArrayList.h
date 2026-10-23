#ifndef __ARRAY_LIST__
#define __ARRAY_LIST__

#include <stddef.h>
#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/Object.h"
#include "java/util/Comparator.h"

template<class T>
class ArrayList : public Object {
private:
    long int increaseChunk;
    long int currentSize;
    long int maxSize;
    T *Data;

    void mergeSort(T *buffer, long int from, long int to,
                   const Comparator<T> &comparator);

    void init();
    void copyFrom(const ArrayList& other);
    void assignFrom(const ArrayList& other);

public:
    // C++ port note: the constructors, the assignment and the destructor are
    // defined in the class body, calling members defined out of it: gcc
    // 2.7.2 does not instantiate the out of class ones when they are only
    // used by the copy constructors and assignments it synthesizes
    ArrayList()
    {
        // Storage is allocated on the first insertion, as many lists of the
        // port stay empty (saves memory on 16 bit targets)
        currentSize = 0;
        increaseChunk = 100;
        maxSize = 0;
        init();
    }
    explicit ArrayList(long i)
    {
        currentSize = 0;
        increaseChunk = i;
        maxSize = increaseChunk;
        init();
    }
    ArrayList(const ArrayList& other) : Data(NULL)
    {
        copyFrom(other);
    }

    ArrayList& operator=(const ArrayList& other)
    {
        assignFrom(other);
        return *this;
    }

    ~ArrayList()
    {
        dispose();
    }

    void dispose();

    long int size() const;
    BOOLEAN isEmpty() const;
    T get(long int i) const;

    BOOLEAN add(T elem);
    T &operator[](long int i);
    const T &operator[](long int i) const;
    T *data();
    const T *data() const;
    void reserve(long int n);
    void add(long int pos, T elem);
    void remove(long int pos);
    void remove(T data);
    void set(long int pos, T elem);
    void clear();

    /**
    @return the index of the first element equal (`operator==`) to `elem`,
    or -1
    */
    long int indexOf(const T &elem) const;
    BOOLEAN contains(const T &elem) const;
    void addAll(const ArrayList &other);

    /**
    @return true if both lists have equal (`operator==`) elements in the
    same order (Java `List.equals`)
    */
    BOOLEAN equals(const ArrayList &other) const;

    /**
    Emulation of `java.util.List.sort`: stable merge sort (equal elements
    keep their relative order).
    */
    void sort(const Comparator<T> &comparator);
};

#include "java/util/ArrayList.txx"

#endif
