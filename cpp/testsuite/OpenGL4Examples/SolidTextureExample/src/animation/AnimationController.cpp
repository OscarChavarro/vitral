#include <algorithm>
#include <chrono>
#include <cmath>

#include "animation/AnimationController.h"
#include "model/SolidTextureModel.h"

static const double FULL_ROTATION_RADIANS = 2.0 * M_PI;
static const double ROTATION_PERIOD_SECONDS = 8.0;
static const double ANGULAR_SPEED_RAD_PER_SECOND =
    FULL_ROTATION_RADIANS / ROTATION_PERIOD_SECONDS;
static const double MAX_ELAPSED_SECONDS = 0.25;

AnimationController::AnimationController() : running(false) {}
AnimationController::~AnimationController() { stop(); }

void AnimationController::start(
    SolidTextureModel* model, const std::function<void()>& repaintCallback)
{
    if ( running || model == 0 ) return;
    running = true;
    worker = std::thread([this, model, repaintCallback]() {
        typedef std::chrono::steady_clock clock;
        clock::time_point startT = clock::now();
        clock::time_point lastT = startT;
        clock::time_point lastGizmoT = startT;
        while ( running ) {
            std::this_thread::sleep_for(std::chrono::milliseconds(16));
            clock::time_point now = clock::now();
            double elapsed = std::chrono::duration<double>(now - lastT).count();
            lastT = now;
            bool gizmoUpdated = false;
            if ( std::chrono::duration<double>(now - lastGizmoT).count() >= 1.0 ) {
                model->getRayGizmo()->update();
                model->getInfinitePlaneGizmo()->update();
                lastGizmoT = now;
                gizmoUpdated = true;
            }
            if ( model->isAnimationEnabled() ) {
                elapsed = std::max(0.0, std::min(elapsed, MAX_ELAPSED_SECONDS));
                model->advanceObjectRotationRadians(ANGULAR_SPEED_RAD_PER_SECOND * elapsed);
                if ( repaintCallback ) repaintCallback();
            }
            else if ( gizmoUpdated && repaintCallback ) {
                repaintCallback();
            }
        }
    });
}

void AnimationController::stop()
{
    running = false;
    if ( worker.joinable() ) worker.join();
}
