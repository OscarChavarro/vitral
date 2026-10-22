#include <stdio.h>
#include "java/util/ArrayDeque.h"
#include <fcntl.h>
#include <pthread.h>
#include <unistd.h>

// Xt names its strings and objects as the vitral String and Object
// classes: they are renamed while its headers are read (see
// XtIntrinsics.h)
#define String XtString
#define Object XtObject
#include <X11/Intrinsic.h>
#undef Object
#undef String

#include "vsdk/toolkit/gui/XtEventQueue.h"
#include "java/lang/Exception.h"


struct XtEventQueueTask {
    Runnable* runnable;
    bool owned;
    unsigned long delayMillis;
    /// Set when an `invokeAndWait` task ended, or null
    bool* done;
};

/**
State of the queue. Never destroyed: threads may still wait on it while the
process exits.
*/
struct XtEventQueueState {
    XtAppContext appContext;
    int pipeFds[2];
    pthread_t dispatchThread;
    bool installed;
    pthread_mutex_t mutex;
    pthread_cond_t finished;
    ArrayDeque<XtEventQueueTask> tasks;

    XtEventQueueState() : appContext(NULL), installed(false)
    {
        pipeFds[0] = -1;
        pipeFds[1] = -1;
        pthread_mutex_init(&mutex, NULL);
        pthread_cond_init(&finished, NULL);
    }
};

/**
Holds the mutex of the queue in a scope.
*/
class XtEventQueueLock {
public:
    explicit XtEventQueueLock(pthread_mutex_t* mutex) : mutex(mutex)
    {
        pthread_mutex_lock(mutex);
    }
    ~XtEventQueueLock()
    {
        pthread_mutex_unlock(mutex);
    }
private:
    pthread_mutex_t* mutex;
};

static XtEventQueueState* state()
{
    static XtEventQueueState* queue = new XtEventQueueState();
    return queue;
}

static void runTask(Runnable* runnable)
{
    try {
        runnable->run();
    }
    catch ( const Exception& e ) {
        fprintf(stderr, "XtEventQueue: task failed: %s\n", e.getMessage().c_str());
    }
}

static void finishTask(const XtEventQueueTask& task)
{
    if ( task.owned ) {
        delete task.runnable;
    }
    if ( task.done != NULL ) {
        XtEventQueueState* queue = state();
        XtEventQueueLock lock(&queue->mutex);
        *task.done = true;
        pthread_cond_broadcast(&queue->finished);
    }
}

static void runDelayedTask(XtPointer clientData, XtIntervalId*)
{
    XtEventQueueTask* task = ((XtEventQueueTask*)(clientData));
    runTask(task->runnable);
    finishTask(*task);
    delete task;
}

static void dispatchTasks(XtPointer, int* fd, XtInputId*)
{
    XtEventQueueState* queue = state();
    char wakeUps[64];

    // One byte per task, but they are all taken below
    if ( read(*fd, wakeUps, sizeof(wakeUps)) <= 0 ) {
        return;
    }
    while ( true ) {
        XtEventQueueTask task;
        {
            XtEventQueueLock lock(&queue->mutex);
            if ( queue->tasks.isEmpty() ) {
                break;
            }
            task = queue->tasks.removeFirst();
        }
        if ( task.delayMillis > 0 ) {
            XtAppAddTimeOut(queue->appContext, task.delayMillis,
                            &runDelayedTask, new XtEventQueueTask(task));
            continue;
        }
        runTask(task.runnable);
        finishTask(task);
    }
}

static void post(const XtEventQueueTask& task)
{
    XtEventQueueState* queue = state();
    {
        XtEventQueueLock lock(&queue->mutex);
        queue->tasks.addLast(task);
    }
    char wakeUp = 1;
    if ( write(queue->pipeFds[1], &wakeUp, 1) < 0 ) {
        perror("XtEventQueue: write");
    }
}


bool XtEventQueue::install(XtAppContext appContext)
{
    XtEventQueueState* queue = state();
    if ( pipe(queue->pipeFds) != 0 ) {
        perror("XtEventQueue: pipe");
        return false;
    }
    // The loop reads the pipe only when it has data: never blocks on it
    fcntl(queue->pipeFds[0], F_SETFL, O_NONBLOCK);
    queue->appContext = appContext;
    queue->dispatchThread = pthread_self();
    queue->installed = true;
    XtAppAddInput(appContext, queue->pipeFds[0],
                  ((XtPointer)(XtInputReadMask)),
                  &dispatchTasks, NULL);
    return true;
}

bool XtEventQueue::isDispatchThread()
{
    XtEventQueueState* queue = state();
    return queue->installed &&
        pthread_equal(pthread_self(), queue->dispatchThread);
}

void XtEventQueue::invokeAndWait(Runnable* task)
{
    if ( isDispatchThread() ) {
        runTask(task);
        return;
    }
    XtEventQueueState* queue = state();
    bool done = false;
    XtEventQueueTask entry = { task, false, 0, &done };
    post(entry);
    XtEventQueueLock lock(&queue->mutex);
    while ( !done ) {
        pthread_cond_wait(&queue->finished, &queue->mutex);
    }
}

void XtEventQueue::invokeLater(Runnable* task)
{
    XtEventQueueTask entry = { task, true, 0, NULL };
    post(entry);
}

void XtEventQueue::invokeLater(Runnable* task, unsigned long delayMillis)
{
    XtEventQueueTask entry = { task, true, delayMillis, NULL };
    post(entry);
}
