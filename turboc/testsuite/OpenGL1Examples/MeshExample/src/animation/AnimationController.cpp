#include <stddef.h>

#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/Runnable.h"
#include "java/lang/Thread.h"
#include "animation/AnimationController.h"
#include "model/MeshModel.h"

AnimationController::AnimationController()
    : model(NULL), repaintCallback(NULL), started(FALSE), stopRequested(FALSE)
{
}

AnimationController::~AnimationController()
{
    stop();
}

void AnimationController::start(MeshModel* model, Runnable* repaintCallback)
{
    if ( started ) {
        return;
    }

    this->model = model;
    this->repaintCallback = repaintCallback;
    if ( pthread_create(&thread, NULL, threadEntry, this) == 0 ) {
        started = TRUE;
    }
}

void AnimationController::stop()
{
    stopRequested = TRUE;
    if ( started && !pthread_equal(pthread_self(), thread) ) {
        pthread_join(thread, NULL);
        started = FALSE;
    }
}

void* AnimationController::threadEntry(void* arg)
{
    AnimationController* controller = (AnimationController*)arg;
    if ( controller != NULL ) {
        controller->loop();
    }
    return NULL;
}

void AnimationController::loop()
{
    while ( !stopRequested ) {
        Thread::sleep(1000);
        if ( model != NULL ) {
            model->getRayGizmo()->update();
        }
        if ( !stopRequested && repaintCallback != NULL ) {
            repaintCallback->run();
        }
    }
}
