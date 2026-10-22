#ifndef __FUTURE_TASK__
#define __FUTURE_TASK__

#include "java/lang/PosixThreads.h"

#include "java/lang/Exception.h"
#include "java/lang/Runnable.h"
#include "java/util/concurrent/Callable.h"
#include "java/util/concurrent/ExecutorService.h"
#include "java/util/concurrent/Future.h"
#include "vsdk/toolkit/common/memoryManagement/SharedPointer.h"

/**
Emulation of `java.util.concurrent.FutureTask`: runs a `Callable` and
publishes its result (or its failure) to the `Future` of the task.
*/
template<class T>
class FutureTask : public Runnable {
private:
    Callable<T>* callable_;
    SharedPointer<FutureState<T> > state_;

    void finish(const T* value, const char* errorMessage)
    {
        pthread_mutex_lock(&state_->mutex);
        if ( value != 0 ) {
            state_->value = *value;
        }
        else {
            state_->failed = true;
            state_->errorMessage = errorMessage;
        }
        state_->done = true;
        pthread_cond_broadcast(&state_->cond);
        pthread_mutex_unlock(&state_->mutex);
    }

public:
    /**
    @param callable work to do, deleted after it runs
    */
    explicit FutureTask(Callable<T>* callable)
        : callable_(callable), state_(new FutureState<T>()) {}

    Future<T> getFuture() const
    {
        return Future<T>(state_);
    }

    virtual void run()
    {
        try {
            T value = callable_->call();
            finish(&value, 0);
        }
        catch (const Exception& e) {
            finish(0, e.getMessage().c_str());
        }
        catch (...) {
            finish(0, "unknown exception");
        }
        delete callable_;
    }

    /**
    Emulation of `java.util.concurrent.ExecutorService.submit(Callable)`.
    @param executor executor to run the task
    @param callable work to do, deleted after it runs
    @return the future result of the work
    */
    static Future<T> submit(ExecutorService* executor, Callable<T>* callable)
    {
        FutureTask<T>* task = new FutureTask<T>(callable);
        Future<T> future = task->getFuture();
        if ( !executor->execute(task) ) {
            task->finish(0, "executor rejected task");
            delete task;
        }
        return future;
    }
};


#endif
