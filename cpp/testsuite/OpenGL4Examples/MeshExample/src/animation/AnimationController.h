#ifndef __ANIMATION_CONTROLLER__
#define __ANIMATION_CONTROLLER__

#include <functional>
#include <pthread.h>

class MeshModel;

class AnimationController {
private:
    MeshModel* model;
    std::function<void()> repaintCallback;
    bool started;
    bool stopRequested;
    pthread_t thread;

    static void* threadEntry(void* arg);
    void loop();

public:
    AnimationController();
    ~AnimationController();
    void start(MeshModel* model, std::function<void()> repaintCallback);
    void stop();
};

#endif
