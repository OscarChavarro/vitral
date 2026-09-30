#ifndef __SOLID_TEXTURE_ANIMATION_CONTROLLER__
#define __SOLID_TEXTURE_ANIMATION_CONTROLLER__

#include <atomic>
#include <functional>
#include <thread>

class SolidTextureModel;

class AnimationController {
private:
    std::atomic<bool> running;
    std::thread worker;

public:
    AnimationController();
    ~AnimationController();
    void start(SolidTextureModel* model, const std::function<void()>& repaintCallback);
    void stop();
};

#endif
