#ifndef __ANIMATION__
#define __ANIMATION__

class ShadersModel;
class Light;

class Animation {
public:
    static const int FRAMES_PER_SECOND = 30;
    static const int FRAME_DELAY_MILLIS = 1000 / FRAMES_PER_SECOND;

    Animation();
    void reset();
    void tick(ShadersModel* model, double nowSeconds);
    void tickForApp(
        double* sphereAngleRadians,
        bool animationEnabled,
        Light* light,
        bool lightAnimationEnabled,
        double nowSeconds);

private:
    double lastSphereTickSeconds;
    double lastLightTickSeconds;
    double lightAnimationAngleRadians;
};

#endif
