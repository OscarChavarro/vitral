#include "java/lang/Math.h"
#include "java/lang/Runnable.h"
#include "java/lang/System.h"
#include "java/lang/Thread.h"
#include <cmath>

#include "animation/AnimationController.h"
#include "model/SolidTextureModel.h"

static const double FULL_ROTATION_RADIANS = 2.0 * M_PI;
static const double ROTATION_PERIOD_SECONDS = 8.0;
static const double ANGULAR_SPEED_RAD_PER_SECOND =
    FULL_ROTATION_RADIANS / ROTATION_PERIOD_SECONDS;
static const double MAX_ELAPSED_SECONDS = 0.25;

AnimationController::AnimationController() : task(nullptr), worker(nullptr) {}
AnimationController::~AnimationController() { stop(); }

void AnimationController::start(
    SolidTextureModel* model, const java::Runnable& repaintCallback)
{
    if ( running.get() != 0 || model == 0 ) return;
    running.set(1);
    // A copy that the worker may run (the parameter is const)
    java::Runnable repaint = repaintCallback;
    task = new java::Runnable([this, model, repaint]() mutable {
        long long startT = java::System::nanoTime();
        long long lastT = startT;
        long long lastGizmoT = startT;
        while ( running.get() != 0 ) {
            java::Thread::sleep(16);
            long long now = java::System::nanoTime();
            double elapsed = (double)(now - lastT) / 1.0e9;
            lastT = now;
            bool gizmoUpdated = false;
            if ( (double)(now - lastGizmoT) / 1.0e9 >= 1.0 ) {
                model->getRayGizmo()->update();
                model->getInfinitePlaneGizmo()->update();
                lastGizmoT = now;
                gizmoUpdated = true;
            }
            if ( model->isAnimationEnabled() ) {
                elapsed = java::Math::max(0.0, java::Math::min(elapsed, MAX_ELAPSED_SECONDS));
                model->advanceObjectRotationRadians(ANGULAR_SPEED_RAD_PER_SECOND * elapsed);
                if ( repaint ) repaint.run();
            }
            else if ( gizmoUpdated && repaint ) {
                repaint.run();
            }
        }
    });
    worker = new java::Thread(task);
    worker->start();
}

void AnimationController::stop()
{
    running.set(0);
    if ( worker != nullptr ) {
        worker->join();
        delete worker;
        worker = nullptr;
        delete task;
        task = nullptr;
    }
}
