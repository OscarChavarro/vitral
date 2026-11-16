#ifndef __ANIMATION_LISTENER__
#define __ANIMATION_LISTENER__

#include "vsdk/toolkit/animation/AnimationEvent.h"

/// C++ port of Java's `vsdk.toolkit.animation.AnimationListener`
class AnimationListener {
public:
    virtual ~AnimationListener() {}

    virtual void tick(const AnimationEvent& e) = 0;

    virtual bool isPaused() const
    {
        return false;
    }

    virtual void setPaused(bool /*paused*/)
    {
    }
};

#endif
