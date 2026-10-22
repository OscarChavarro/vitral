#ifndef __VITRAL_RAYTRACER__
#define __VITRAL_RAYTRACER__

#ifndef VITRAL_RAYTRACER_DEFAULT_SCENE
#error "VITRAL_RAYTRACER_DEFAULT_SCENE (scene shown when none is given) is defined by CMake"
#endif

#include "lib/dithering/Dithering.h"
#include "lib/vgaModes/VgaModes.h"
#include "programs/vitralRaytracer/Frame.h"
#include "programs/vitralRaytracer/RaytracerColorMode.h"
#include "programs/vitralRaytracer/RaytracerConstant.h"
#include "programs/vitralRaytracer/RaytracerHud.h"
#include "programs/vitralRaytracer/SceneList.h"

class VitralRaytracer {
public:
    static int start(int argc, char *argv[]);

private:
    explicit VitralRaytracer(VgaModes *modes);
    VitralRaytracer(const VitralRaytracer &);
    VitralRaytracer &operator=(const VitralRaytracer &);

    int run(int mode, const char *scene);
    void useScreen();
    static int waitCommand();

    VgaModes *modes;
    int width;
    int height;
    Dithering::Palette palette;
    SceneList scenes;
    RaytracerHud hud;
    Frame frame;
    RaytracerColorMode colorMode;
    double seconds;
    char error[MAX_PATH + 96];
    int parallel;
};

#endif
