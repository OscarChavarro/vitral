#ifndef __CONCURRENT_LINKED_QUEUE__
#define __CONCURRENT_LINKED_QUEUE__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/util/ArrayDeque.h"
#include "java/util/ArrayList.h"
#include "java/lang/PosixThreads.h"

template<class T>
class ConcurrentLinkedQueue {
private:
    ArrayDeque<T> data_;
    pthread_mutex_t mutex_;

public:
    ConcurrentLinkedQueue() { pthread_mutex_init(&mutex_, 0); }

    explicit ConcurrentLinkedQueue(const ArrayList<T>& initial)
    {
        pthread_mutex_init(&mutex_, 0);
        for (long int i = 0; i < initial.size(); ++i) {
            data_.addLast(initial[i]);
        }
    }

    ~ConcurrentLinkedQueue() { pthread_mutex_destroy(&mutex_); }

    void add(const T& item)
    {
        pthread_mutex_lock(&mutex_);
        data_.addLast(item);
        pthread_mutex_unlock(&mutex_);
    }

    BOOLEAN poll(T* out)
    {
        if ( out == 0 ) return FALSE;
        pthread_mutex_lock(&mutex_);
        if ( data_.isEmpty() ) {
            pthread_mutex_unlock(&mutex_);
            return FALSE;
        }
        *out = data_.peekFirst();
        data_.removeFirst();
        pthread_mutex_unlock(&mutex_);
        return TRUE;
    }

    BOOLEAN isEmpty() const
    {
        // Locking changes the mutex (no `mutable` in Turbo C++ 3.0)
        pthread_mutex_t *mutex = (pthread_mutex_t *)&mutex_;
        pthread_mutex_lock(mutex);
        BOOLEAN empty = data_.isEmpty();
        pthread_mutex_unlock(mutex);
        return empty;
    }
};


#endif
