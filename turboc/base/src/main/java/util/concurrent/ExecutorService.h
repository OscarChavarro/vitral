#ifndef __EXECUTOR_SERVICE__
#define __EXECUTOR_SERVICE__

#include "java/lang/Runnable.h"

/**
Emulation of `java.util.concurrent.ExecutorService`. Tasks returning a
value are submitted with `FutureTask<T>::submit(executor, callable)`.

C++ port note: legacy compilers (gcc 2.7, Turbo C++ 3.0) have no member
templates, so the generic `submit` of Java is a static method of the
`FutureTask` class template.
*/
class ExecutorService {
public:
    virtual ~ExecutorService() {}
    virtual void shutdownNow() = 0;

    /**
    Emulation of `java.util.concurrent.Executor.execute`: runs the task in
    a thread of the pool (or right away, without POSIX threads), and
    deletes it after it runs.
    @return false if the executor rejected the task (then it is not
    deleted)
    */
    virtual bool execute(Runnable* task) = 0;
};


#endif
