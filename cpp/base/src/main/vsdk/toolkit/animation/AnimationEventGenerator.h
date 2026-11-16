#ifndef __ANIMATION_EVENT_GENERATOR__
#define __ANIMATION_EVENT_GENERATOR__

#include "java/lang/Runnable.h"
#include "java/util/ArrayList.h"
#include "java/util/concurrent/atomic/AtomicLong.h"
#include "vsdk/toolkit/animation/AnimationListener.h"

/**
C++ port of Java's `vsdk.toolkit.animation.AnimationEventGenerator`: when
run (usually in its own `java::Thread`), dispatches an `AnimationEvent`
to its listeners 24 times per second.

C++ port note: Java loops forever, in a daemon thread that dies with the
virtual machine. A C++ thread may outlive the objects its listeners use,
so `stop` ends the loop (after the current tick); the listeners are not
owned.
*/
class AnimationEventGenerator : public java::Runnable {
private:
    int fps;
    java::ArrayList<AnimationListener*> listeners;
    java::AtomicLong stopRequested;

    void dispatch(const AnimationEvent& e);
    double getRealTimeSeconds() const;

public:
    AnimationEventGenerator();
    virtual ~AnimationEventGenerator() {}

    void addAnimationListener(AnimationListener* l);

    virtual void run() override;

    /// C++ port: ends `run`
    void stop();
};

#endif
