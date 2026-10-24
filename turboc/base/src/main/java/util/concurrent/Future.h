#ifndef __FUTURE__
#define __FUTURE__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/String.h"
#include "java/util/concurrent/ExecutionException.h"
#include "java/lang/PosixThreads.h"
#include "vsdk/toolkit/common/memoryManagement/SharedPointer.h"

/**
State shared by a `Future` and the `FutureTask` that computes its value.

C++ port note: it is not nested in `Future`, so other templates can name it
without `typename` (unknown to legacy compilers).
*/
template<class T>
class FutureState {
public:
    pthread_mutex_t mutex;
    pthread_cond_t cond;
    BOOLEAN done;
    BOOLEAN failed;
    String errorMessage;
    T value;

    // `value` is only read once `done` is set (gcc 2.7.2 rejects a `value()`
    // initializer when T is a pointer or a class without constructors)
    FutureState() : done(FALSE), failed(FALSE), errorMessage() {
        pthread_mutex_init(&mutex, 0);
        pthread_cond_init(&cond, 0);
    }

    ~FutureState() {
        pthread_cond_destroy(&cond);
        pthread_mutex_destroy(&mutex);
    }
};

template<class T>
class Future {
private:
    SharedPointer<FutureState<T> > state_;

public:
    Future() : state_(new FutureState<T>()) {}
    explicit Future(const SharedPointer<FutureState<T> >& state) : state_(state) {}

    T get()
    {
        pthread_mutex_lock(&state_->mutex);
        while ( !state_->done ) {
            pthread_cond_wait(&state_->cond, &state_->mutex);
        }
        BOOLEAN failed = state_->failed;
        String message = state_->errorMessage;
        T value = state_->value;
        pthread_mutex_unlock(&state_->mutex);

        if ( failed ) {
            if ( message.isEmpty() ) message = String("Task execution failed");
            throw ExecutionException(message);
        }
        return value;
    }
};


#endif
