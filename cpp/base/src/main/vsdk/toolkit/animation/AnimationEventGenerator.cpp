#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/animation/AnimationEventGenerator.h"

#include "java/lang/System.h"
#include "java/lang/Thread.h"

AnimationEventGenerator::AnimationEventGenerator()
{
    fps = 24;
}

void AnimationEventGenerator::addAnimationListener(AnimationListener* l)
{
    listeners.add(l);
}

void AnimationEventGenerator::dispatch(const AnimationEvent& e)
{
    long int i;
    for ( i = 0; i < listeners.size(); i++ ) {
        listeners.get(i)->tick(e);
    }
}

double AnimationEventGenerator::getRealTimeSeconds() const
{
    return (double)java::System::currentTimeMillis()/1000.0;
}

/**
C++ port note: Java names its thread "ANIM: AnimationEventGenerator.run";
`java::Thread` has no `currentThread`, so the thread is not renamed.
*/
void AnimationEventGenerator::run()
{
    double t;
    double t0 = getRealTimeSeconds();
    AnimationEvent e;
    while ( stopRequested.get() == 0 ) {
        t = getRealTimeSeconds() - t0;
        e.setTime(t);
        java::Thread::sleep(1000/fps);
        if ( stopRequested.get() != 0 ) {
            break;
        }
        dispatch(e);
    }
}

void AnimationEventGenerator::stop()
{
    stopRequested.set(1);
}
