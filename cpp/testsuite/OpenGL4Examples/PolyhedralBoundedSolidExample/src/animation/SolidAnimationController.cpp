#include "animation/SolidAnimationController.h"

#include "java/lang/Thread.h"
#include "models/DebuggerModel.h"

static const double ROTATION_STEP_DEGREES = 1.0;

/// Java: the anonymous `AnimationListener`
class SolidAnimationController::Listener : public AnimationListener {
public:
    Listener(DebuggerModel* model, const java::Runnable& repaint)
        : model(model), repaint(repaint)
    {
    }

    virtual void tick(const AnimationEvent& /*e*/) override
    {
        if ( !model->isSolidAnimationEnabled() ) {
            return;
        }
        model->rotateSolidAroundZDegrees(ROTATION_STEP_DEGREES);
        if ( repaint ) {
            repaint.run();
        }
    }

private:
    DebuggerModel* model;
    java::Runnable repaint;
};

SolidAnimationController::SolidAnimationController()
    : animator(nullptr), listener(nullptr), thread(nullptr), started(false)
{
}

SolidAnimationController::~SolidAnimationController()
{
    stop();
}

void SolidAnimationController::start(DebuggerModel* model,
    const java::Runnable& repaint)
{
    if ( started ) {
        return;
    }

    animator = new AnimationEventGenerator();
    listener = new Listener(model, repaint);
    animator->addAnimationListener(listener);

    thread = new java::Thread(animator);
    thread->start();
    started = true;
}

void SolidAnimationController::toggleAnimation(DebuggerModel& model)
{
    model.toggleSolidAnimationEnabled();
}

void SolidAnimationController::stop()
{
    if ( !started ) {
        return;
    }
    animator->stop();
    thread->join();
    delete thread;
    delete listener;
    delete animator;
    thread = nullptr;
    listener = nullptr;
    animator = nullptr;
    started = false;
}
