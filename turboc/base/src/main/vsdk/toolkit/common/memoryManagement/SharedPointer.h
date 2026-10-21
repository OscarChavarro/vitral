#ifndef __SHARED_POINTER__
#define __SHARED_POINTER__

#include <stddef.h>

#include "java/util/concurrent/locks/ReentrantLock.h"

/**
Reference counted pointer, C++98 replacement for `std::shared_ptr`: the
pointed object is deleted when the last `SharedPointer` referencing it is
destroyed or reset. The reference counter is thread safe, but (as for
`std::shared_ptr`) a single `SharedPointer` object must not be modified by
one thread while other threads access it.
*/
template<typename T>
class SharedPointer {
  private:
    struct ControlBlock {
        ReentrantLock lock;
        long references;
        ControlBlock() : lock(), references(1) {}
    };

    T *pointer;
    ControlBlock *control;

    void retain()
    {
        if ( control != NULL ) {
            ReentrantLockGuard guard(control->lock);
            control->references++;
        }
    }

    void release()
    {
        if ( control == NULL ) {
            return;
        }
        bool last;
        {
            ReentrantLockGuard guard(control->lock);
            control->references--;
            last = control->references == 0;
        }
        if ( last ) {
            delete pointer;
            delete control;
        }
        pointer = NULL;
        control = NULL;
    }

  public:
    SharedPointer() : pointer(NULL), control(NULL) {}

    explicit SharedPointer(T *inPointer) :
        pointer(inPointer), control(inPointer != NULL ? new ControlBlock() : NULL)
    {
    }

    SharedPointer(const SharedPointer<T> &other) :
        pointer(other.pointer), control(other.control)
    {
        retain();
    }

    ~SharedPointer()
    {
        release();
    }

    SharedPointer<T> &operator=(const SharedPointer<T> &other)
    {
        if ( control != other.control ) {
            SharedPointer<T> copy(other);
            release();
            pointer = copy.pointer;
            control = copy.control;
            copy.pointer = NULL;
            copy.control = NULL;
        }
        return *this;
    }

    void reset()
    {
        release();
    }

    T *get() const
    {
        return pointer;
    }

    T *operator->() const
    {
        return pointer;
    }

    T &operator*() const
    {
        return *pointer;
    }

    bool operator!() const
    {
        return pointer == NULL;
    }
};

#endif
