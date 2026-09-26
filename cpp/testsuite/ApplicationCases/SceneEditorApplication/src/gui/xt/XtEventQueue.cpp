#include <condition_variable>
#include <cstdio>
#include <deque>
#include <exception>
#include <mutex>
#include <thread>
#include <fcntl.h>
#include <unistd.h>

#include <X11/Intrinsic.h>

#include "gui/xt/XtEventQueue.h"

namespace {

struct Task {
    java::Runnable* runnable;
    bool owned;
    unsigned long delayMillis;
    /// Set when an `invokeAndWait` task ended, or null
    bool* done;
};

/**
State of the queue. Never destroyed: threads may still wait on it while the
process exits.
*/
struct QueueState {
    XtAppContext appContext;
    int pipeFds[2];
    std::thread::id dispatchThread;
    std::mutex mutex;
    std::condition_variable finished;
    std::deque<Task> tasks;

    QueueState() : appContext(nullptr)
    {
        pipeFds[0] = -1;
        pipeFds[1] = -1;
    }
};

QueueState* state()
{
    static QueueState* queue = new QueueState();
    return queue;
}

void runTask(java::Runnable* runnable)
{
    try {
        runnable->run();
    }
    catch ( const std::exception& e ) {
        fprintf(stderr, "XtEventQueue: task failed: %s\n", e.what());
    }
}

void finishTask(const Task& task)
{
    if ( task.owned ) {
        delete task.runnable;
    }
    if ( task.done != nullptr ) {
        QueueState* queue = state();
        std::lock_guard<std::mutex> lock(queue->mutex);
        *task.done = true;
        queue->finished.notify_all();
    }
}

void runDelayedTask(XtPointer clientData, XtIntervalId*)
{
    Task* task = static_cast<Task*>(clientData);
    runTask(task->runnable);
    finishTask(*task);
    delete task;
}

void dispatchTasks(XtPointer, int* fd, XtInputId*)
{
    QueueState* queue = state();
    char wakeUps[64];

    // One byte per task, but they are all taken below
    if ( read(*fd, wakeUps, sizeof(wakeUps)) <= 0 ) {
        return;
    }
    while ( true ) {
        Task task;
        {
            std::lock_guard<std::mutex> lock(queue->mutex);
            if ( queue->tasks.empty() ) {
                break;
            }
            task = queue->tasks.front();
            queue->tasks.pop_front();
        }
        if ( task.delayMillis > 0 ) {
            XtAppAddTimeOut(queue->appContext, task.delayMillis,
                            &runDelayedTask, new Task(task));
            continue;
        }
        runTask(task.runnable);
        finishTask(task);
    }
}

void post(const Task& task)
{
    QueueState* queue = state();
    {
        std::lock_guard<std::mutex> lock(queue->mutex);
        queue->tasks.push_back(task);
    }
    char wakeUp = 1;
    if ( write(queue->pipeFds[1], &wakeUp, 1) < 0 ) {
        perror("XtEventQueue: write");
    }
}

}

bool XtEventQueue::install(XtAppContext appContext)
{
    QueueState* queue = state();
    if ( pipe(queue->pipeFds) != 0 ) {
        perror("XtEventQueue: pipe");
        return false;
    }
    // The loop reads the pipe only when it has data: never blocks on it
    fcntl(queue->pipeFds[0], F_SETFL, O_NONBLOCK);
    queue->appContext = appContext;
    queue->dispatchThread = std::this_thread::get_id();
    XtAppAddInput(appContext, queue->pipeFds[0],
                  reinterpret_cast<XtPointer>(XtInputReadMask),
                  &dispatchTasks, nullptr);
    return true;
}

bool XtEventQueue::isDispatchThread()
{
    return std::this_thread::get_id() == state()->dispatchThread;
}

void XtEventQueue::invokeAndWait(java::Runnable* task)
{
    if ( isDispatchThread() ) {
        runTask(task);
        return;
    }
    QueueState* queue = state();
    bool done = false;
    Task entry = { task, false, 0, &done };
    post(entry);
    std::unique_lock<std::mutex> lock(queue->mutex);
    queue->finished.wait(lock, [&done]() { return done; });
}

void XtEventQueue::invokeLater(java::Runnable* task)
{
    Task entry = { task, true, 0, nullptr };
    post(entry);
}

void XtEventQueue::invokeLater(java::Runnable* task, unsigned long delayMillis)
{
    Task entry = { task, true, delayMillis, nullptr };
    post(entry);
}
