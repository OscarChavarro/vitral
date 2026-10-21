#ifndef __ANIMATION_CONTROLLER__
#define __ANIMATION_CONTROLLER__

#include <pthread.h>

class MeshModel;
class Runnable;

class AnimationController {
private:
    MeshModel* model;
    Runnable* repaintCallback;
    bool started;
    volatile bool stopRequested;
    pthread_t thread;

    static void* threadEntry(void* arg);
    void loop();

public:
    AnimationController();
    ~AnimationController();
    /**
    @param repaintCallback called from the animation thread after each step
    (not owned)
    */
    void start(MeshModel* model, Runnable* repaintCallback);
    void stop();
};

#endif
