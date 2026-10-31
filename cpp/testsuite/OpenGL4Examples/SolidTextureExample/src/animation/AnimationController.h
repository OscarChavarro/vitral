#ifndef __SOLID_TEXTURE_ANIMATION_CONTROLLER__
#define __SOLID_TEXTURE_ANIMATION_CONTROLLER__

#include "java/lang/Runnable.h"
#include "java/lang/Thread.h"
#include "java/util/concurrent/atomic/AtomicLong.h"

class SolidTextureModel;

class AnimationController {
private:
    /// 1 while the worker thread runs
    java::AtomicLong running;
    java::Runnable* task;
    java::Thread* worker;

public:
    AnimationController();
    ~AnimationController();
    void start(SolidTextureModel* model, const java::Runnable& repaintCallback);
    void stop();
};

#endif
