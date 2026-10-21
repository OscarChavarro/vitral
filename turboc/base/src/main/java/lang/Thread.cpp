#include <stddef.h>
#include <time.h>

#include "java/lang/Thread.h"



/**
Startup data of a new thread: the thread waits until `started` is unlocked,
i.e. until `Thread::start` knows it, as its task may destroy the `Thread`.
*/
struct ThreadStartup {
    Runnable *task;
    pthread_mutex_t started;
};

static void *
runThreadStartup(void *data)
{
    ThreadStartup *startup = (ThreadStartup *)data;
    pthread_mutex_lock(&startup->started);
    pthread_mutex_unlock(&startup->started);
    Runnable *task = startup->task;
    pthread_mutex_destroy(&startup->started);
    delete startup;
    if ( task != NULL ) {
        task->run();
    }
    return NULL;
}


Thread::Thread(Runnable *target) :
    target(target), name("Thread"), thread(), joinable(false)
{
}

Thread::~Thread()
{
    if ( joinable ) {
        pthread_detach(thread);
        joinable = false;
    }
}

void
Thread::setName(const String &name)
{
    this->name = name;
}

const String &
Thread::getName() const
{
    return name;
}

void
Thread::start()
{
    // The new thread waits until this object knows it: then its task may
    // destroy this object
    ThreadStartup *startup = new ThreadStartup();
    startup->task = target;
    pthread_mutex_init(&startup->started, NULL);
    pthread_mutex_lock(&startup->started);
    pthread_t newThread;
    if ( pthread_create(&newThread, NULL, runThreadStartup, startup) != 0 ) {
        pthread_mutex_unlock(&startup->started);
        pthread_mutex_destroy(&startup->started);
        delete startup;
        return;
    }
    thread = newThread;
    joinable = true;
    pthread_mutex_unlock(&startup->started);
}

void
Thread::join()
{
    if ( joinable && !pthread_equal(thread, pthread_self()) ) {
        pthread_join(thread, NULL);
        joinable = false;
    }
}

void
Thread::sleep(long long millis)
{
    struct timespec duration;
    duration.tv_sec = (time_t)(millis / 1000);
    duration.tv_nsec = (long)((millis % 1000) * 1000000);
    while ( nanosleep(&duration, &duration) != 0 ) {
    }
}

