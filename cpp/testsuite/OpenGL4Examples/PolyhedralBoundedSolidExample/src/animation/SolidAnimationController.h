#ifndef __POLYHEDRAL_EXAMPLE_SOLID_ANIMATION_CONTROLLER__
#define __POLYHEDRAL_EXAMPLE_SOLID_ANIMATION_CONTROLLER__

#include "java/lang/Runnable.h"
#include "vsdk/toolkit/animation/AnimationEventGenerator.h"

class DebuggerModel;

namespace java {
class Thread;
}

/**
C++ port of Java's `animation.SolidAnimationController`.

C++ port notes: the Java `GLCanvas.repaint` is the `repaint` callback, and
`stop` (called on exit) ends the animation thread, which Java leaves to die
with the virtual machine.
*/
class SolidAnimationController {
public:
    SolidAnimationController();
    ~SolidAnimationController();

    void start(DebuggerModel* model, const java::Runnable& repaint);
    void toggleAnimation(DebuggerModel& model);
    void stop();

private:
    class Listener;

    AnimationEventGenerator* animator;
    Listener* listener;
    java::Thread* thread;
    bool started;
};

#endif
