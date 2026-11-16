#ifndef __ANIMATION_EVENT__
#define __ANIMATION_EVENT__

/// C++ port of Java's `vsdk.toolkit.animation.AnimationEvent`
class AnimationEvent {
private:
    double time;

public:
    AnimationEvent() : time(0.0) {}

    /**
    @return the time of the event
    */
    double getTime() const
    {
        return time;
    }

    /**
    @param value the time of the event
    */
    void setTime(double value)
    {
        this->time = value;
    }
};

#endif
