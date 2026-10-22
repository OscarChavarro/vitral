#ifndef __ARRAY_LIST__
#define __ARRAY_LIST__

#include <stddef.h>
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

    void init()
    {
        if (maxSize > 0) {
            Data = new T[maxSize];
            if (!Data) {
                maxSize = 0;
            }
        } else {
            Data = NULL;
        }
        currentSize = 0;
    }

public:
    ArrayList();
    explicit ArrayList(long i)
    {
        currentSize = 0;
        increaseChunk = i;
        maxSize = increaseChunk;
        init();
    }
    ArrayList(const ArrayList& other);
    ArrayList& operator=(const ArrayList& other);
    ~ArrayList();

    void dispose();

    long int size() const;
    bool isEmpty() const;
    T get(long int i) const;

    bool add(T elem);
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
    bool contains(const T &elem) const;
    void addAll(const ArrayList &other);

    /**
    @return true if both lists have equal (`operator==`) elements in the
    same order (Java `List.equals`)
    */
    bool equals(const ArrayList &other) const;

    /**
    Emulation of `java.util.List.sort`: stable merge sort (equal elements
    keep their relative order).
    */
    void sort(const Comparator<T> &comparator);
};

#include "java/util/ArrayList.txx"

#endif
