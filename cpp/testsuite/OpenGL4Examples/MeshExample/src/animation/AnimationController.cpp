#include "java/lang/Runnable.h"
#include "java/lang/Thread.h"

#include "animation/AnimationController.h"
#include "model/MeshModel.h"

AnimationController::AnimationController()
    : model(0), started(false), stopRequested(false)
{
}

AnimationController::~AnimationController()
{
    stop();
}

void AnimationController::start(MeshModel* model, java::Runnable repaintCallback)
{
    if ( started ) {
        return;
    }

    this->model = model;
    this->repaintCallback = repaintCallback;
    if ( pthread_create(&thread, 0, threadEntry, this) == 0 ) {
        started = true;
    }
}

void AnimationController::stop()
{
    stopRequested = true;
    if ( started && !pthread_equal(pthread_self(), thread) ) {
        pthread_join(thread, 0);
        started = false;
    }
}

void* AnimationController::threadEntry(void* arg)
{
    AnimationController* controller = static_cast<AnimationController*>(arg);
    if ( controller != 0 ) {
        controller->loop();
    }
    return 0;
}

void AnimationController::loop()
{
    while ( !stopRequested ) {
        java::Thread::sleep(1000);
        if ( model != 0 ) {
            model->getRayGizmo()->update();
        }
        if ( !stopRequested ) {
            repaintCallback.run();
        }
    }
}
