#ifndef __XT_EVENT_QUEUE__
#define __XT_EVENT_QUEUE__

#include "java/lang/Runnable.h"

// As declared by <X11/Intrinsic.h>, so this header can be used without Xt
// (whose `Widget` collides with the vitral one)
typedef struct _XtAppStruct* XtAppContext;

/**
Executes tasks in the thread of the Xt event loop, as
`javax.swing.SwingUtilities` and `javax.swing.Timer` do for the event
dispatch thread of AWT: other threads (i.e. the connections of the
automation service) must not touch the GUI nor the model it presents.

The tasks are passed through a pipe watched by the event loop
(`XtAppAddInput`), the only Xt way to wake it from another thread.
*/
class XtEventQueue {
public:
    /**
    Starts serving tasks in the event loop of the application context. Must
    be called from the thread that runs that loop, before the other
    methods.
    @return false if the pipe could not be created
    */
    static bool install(XtAppContext appContext);

    /**
    @return true if called from the thread of the event loop
    */
    static bool isDispatchThread();

    /**
    Runs the task in the thread of the event loop and waits for its end (as
    `SwingUtilities.invokeAndWait`); from that thread it runs it at once.
    The task should catch its exceptions: the ones escaping it are reported
    and dropped.
    @param task task to run (not owned)
    */
    static void invokeAndWait(java::Runnable* task);

    /**
    Runs the task in the thread of the event loop, once the current events
    are processed (as `SwingUtilities.invokeLater`).
    @param task task to run, deleted after it
    */
    static void invokeLater(java::Runnable* task);

    /**
    Runs the task in the thread of the event loop after a delay (as a non
    repeating `javax.swing.Timer`).
    @param task task to run, deleted after it
    @param delayMillis delay in milliseconds
    */
    static void invokeLater(java::Runnable* task, unsigned long delayMillis);

private:
    XtEventQueue();
};

#endif
