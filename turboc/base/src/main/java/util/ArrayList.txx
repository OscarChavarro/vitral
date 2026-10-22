#ifndef __ARRAY_LIST_TXX__
#define __ARRAY_LIST_TXX__

#include <stddef.h>
#include "java/util/ArrayList.h"

template <class T>
ArrayList<T>::ArrayList() {
    // Storage is allocated on the first insertion, as many lists of the
    // port stay empty (saves memory on 16 bit targets)
    currentSize = 0;
    increaseChunk = 100;
    maxSize = 0;
    init();
}

template <class T>
ArrayList<T>::~ArrayList() {
    dispose();
}

template <class T>
void
ArrayList<T>::dispose() {
    if ( Data ) {
        delete[] Data;
        Data = NULL;
    }
    currentSize = 0;
    maxSize = 0;
}

template <class T>
ArrayList<T>::ArrayList(const ArrayList<T>& other)
    : increaseChunk(other.increaseChunk)
    , currentSize(other.currentSize)
    , maxSize(other.maxSize)
    , Data(NULL)
{
    if (maxSize > 0) {
        Data = new T[maxSize];
        for (long int i = 0; i < currentSize; i++) {
            Data[i] = other.Data[i];
        }
    }
}

template <class T> ArrayList<T>&
ArrayList<T>::operator=(const ArrayList<T>& other)
{
    if (this == &other) return *this;
    delete[] Data;
    increaseChunk = other.increaseChunk;
    currentSize = other.currentSize;
    maxSize = other.maxSize;
    Data = NULL;
    if (maxSize > 0) {
        Data = new T[maxSize];
        for (long int i = 0; i < currentSize; i++) {
            Data[i] = other.Data[i];
        }
    }
    return *this;
}

template <class T> bool
ArrayList<T>::add(T voxelData)
{
    if ( currentSize >= maxSize ) {
        // Grows by half of the capacity, as java.util.ArrayList, and at
        // least by the increase chunk: appending is amortized constant time
        long int growth = maxSize / 2;
        if ( growth < increaseChunk ) {
            growth = increaseChunk;
        }
        long int newMaxSize = maxSize + growth;
        if ( newMaxSize <= maxSize ) {
            newMaxSize = maxSize + 1;
        }

        T *newData = new T[newMaxSize];
        if ( !newData ) {
            return false;
        }

        for ( long int i = 0; i < currentSize; i++ ) {
            newData[i] = Data[i];
        }

        delete[] Data;
        Data = newData;
        maxSize = newMaxSize;
    }
    Data[currentSize] = voxelData;
    currentSize++;
    return true;
}

template <class T> long int
ArrayList<T>::size() const {
    return currentSize;
}

template <class T> bool
ArrayList<T>::isEmpty() const {
    return currentSize == 0;
}

template <class T> T &
ArrayList<T>::operator[](long int i) {
    return Data[i];
}

template <class T> const T &
ArrayList<T>::operator[](long int i) const {
    return Data[i];
}

template <class T> T
ArrayList<T>::get(long int i) const {
    return Data[i];
}

template <class T> T*
ArrayList<T>::data() {
    return Data;
}

template <class T> const T*
ArrayList<T>::data() const {
    return Data;
}

template <class T> void
ArrayList<T>::reserve(long int n) {
    if (n <= maxSize) return;
    T* newData = new T[n];
    for (long int i = 0; i < currentSize; i++) {
        newData[i] = Data[i];
    }
    delete[] Data;
    Data = newData;
    maxSize = n;
}

template <class T> void
ArrayList<T>::add(long int pos, T elem)
{
    if ( size() == 0 ) {
        add(elem);
        return;
    }
    T last = get(size() - 1);
    add(last);
    int i;
    for ( i = size() - 1; i >= 1; i-- ) {
        if ( i == pos ) {
            Data[i] = elem;
            break;
        }
        Data[i] = Data[i - 1];
    }
    if ( i <= 0 ) {
        Data[0] = elem;
    }
}

template <class T> void
ArrayList<T>::remove(long int pos)
{
    for ( long i = pos; i < size()-1; i++ ) {
        Data[i] = Data[i+1];
    }
    currentSize--;
}

template <class T> void
ArrayList<T>::remove(T data)
{
    bool shouldRemove = false;
    long int i;
    for ( i = 0; i < size(); i++ ) {
        if ( Data[i] == data ) {
            shouldRemove = true;
            break;
        }
    }

    if ( shouldRemove ) {
        remove(i);
    }
}

template <class T> void
ArrayList<T>::set(long int pos, T elem) {
    Data[pos] = elem;
}

template <class T> void
ArrayList<T>::clear() {
    currentSize = 0;
}

template <class T> long int
ArrayList<T>::indexOf(const T &elem) const {
    for ( long int i = 0; i < currentSize; i++ ) {
        if ( Data[i] == elem ) {
            return i;
        }
    }
    return -1;
}

template <class T> bool
ArrayList<T>::contains(const T &elem) const {
    return indexOf(elem) >= 0;
}

/**
Sorts the elements in [from, to), using `buffer` as temporary storage.
*/
template <class T> void
ArrayList<T>::mergeSort(T *buffer, long int from, long int to,
    const Comparator<T> &comparator)
{
    if ( to - from < 2 ) {
        return;
    }
    long int middle = from + (to - from) / 2;
    mergeSort(buffer, from, middle, comparator);
    mergeSort(buffer, middle, to, comparator);
    if ( comparator.compare(Data[middle - 1], Data[middle]) <= 0 ) {
        return;
    }
    long int i;
    for ( i = from; i < middle; i++ ) {
        buffer[i] = Data[i];
    }
    long int left = from;
    long int right = middle;
    long int target = from;
    while ( left < middle && right < to ) {
        // Takes the right element only when strictly smaller: stable
        if ( comparator.compare(Data[right], buffer[left]) < 0 ) {
            Data[target++] = Data[right++];
        }
        else {
            Data[target++] = buffer[left++];
        }
    }
    while ( left < middle ) {
        Data[target++] = buffer[left++];
    }
}

template <class T> void
ArrayList<T>::sort(const Comparator<T> &comparator) {
    if ( currentSize < 2 ) {
        return;
    }
    T *buffer = new T[currentSize];
    mergeSort(buffer, 0, currentSize, comparator);
    delete[] buffer;
}

template <class T> bool
ArrayList<T>::equals(const ArrayList<T> &other) const {
    if ( currentSize != other.currentSize ) {
        return false;
    }
    for ( long int i = 0; i < currentSize; i++ ) {
        if ( !(Data[i] == other.Data[i]) ) {
            return false;
        }
    }
    return true;
}

template <class T> void
ArrayList<T>::addAll(const ArrayList<T> &other) {
    if ( this == &other ) {
        ArrayList<T> copy(other);
        addAll(copy);
        return;
    }
    reserve(currentSize + other.currentSize);
    for ( long int i = 0; i < other.currentSize; i++ ) {
        add(other.Data[i]);
    }
}


#endif
