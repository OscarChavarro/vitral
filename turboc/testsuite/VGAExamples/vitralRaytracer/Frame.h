#ifndef __FRAME__
#define __FRAME__
#include <stdint.h>
#include "lib/dithering/Dithering.h"
#include "programs/vitralRaytracer/SceneList.h"

class Frame {
public:
    Frame();
    ~Frame();
    Frame(const Frame &) = delete;
    Frame &operator=(const Frame &) = delete;

    int render(const SceneList *scenes, int width, int height, int text, int parallel,
               const Dithering::Palette *palette, double *seconds, char *error, unsigned errorSize);
    const uint8_t *pixels() const { return rgb; }
    int frameWidth() const { return width; }
    int frameHeight() const { return height; }
    int isValid() const { return valid; }

private:
    uint8_t *rgb;
    int width;
    int height;
    int valid;
};

#endif
