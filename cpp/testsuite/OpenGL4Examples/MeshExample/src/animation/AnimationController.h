#ifndef __ANIMATION_CONTROLLER__
#define __ANIMATION_CONTROLLER__

#include "java/lang/Runnable.h"
#include <pthread.h>

class MeshModel;

class AnimationController {
private:
    MeshModel* model;
    java::Runnable repaintCallback;
    bool started;
    bool stopRequested;
    pthread_t thread;

    static void* threadEntry(void* arg);
    void loop();

public:
    AnimationController();
    ~AnimationController();
    void start(MeshModel* model, java::Runnable repaintCallback);
    void stop();
};

#endif
