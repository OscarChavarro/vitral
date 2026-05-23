#ifndef JAVA_UTIL_CONCURRENT_CONCURRENTLINKEDQUEUE_H
#define JAVA_UTIL_CONCURRENT_CONCURRENTLINKEDQUEUE_H

#include <deque>
#include <vector>
#include <pthread.h>

namespace java {

template<typename T>
class ConcurrentLinkedQueue {
private:
    std::deque<T> data_;
    mutable pthread_mutex_t mutex_;

public:
    ConcurrentLinkedQueue() { pthread_mutex_init(&mutex_, 0); }

    explicit ConcurrentLinkedQueue(const std::vector<T>& initial)
    {
        pthread_mutex_init(&mutex_, 0);
        for (size_t i = 0; i < initial.size(); ++i) {
            data_.push_back(initial[i]);
        }
    }

    ~ConcurrentLinkedQueue() { pthread_mutex_destroy(&mutex_); }

    void add(const T& item)
    {
        pthread_mutex_lock(&mutex_);
        data_.push_back(item);
        pthread_mutex_unlock(&mutex_);
    }

    bool poll(T* out)
    {
        if ( out == 0 ) return false;
        pthread_mutex_lock(&mutex_);
        if ( data_.empty() ) {
            pthread_mutex_unlock(&mutex_);
            return false;
        }
        *out = data_.front();
        data_.pop_front();
        pthread_mutex_unlock(&mutex_);
        return true;
    }

    bool isEmpty() const
    {
        pthread_mutex_lock(&mutex_);
        bool empty = data_.empty();
        pthread_mutex_unlock(&mutex_);
        return empty;
    }
};

}

#endif
