#ifndef __GOURAUD_TEXTURE_SHADER__
#define __GOURAUD_TEXTURE_SHADER__
#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/render/shaders/LightingShader.h"
class GouraudTextureShader : public LightingShader { public: explicit GouraudTextureShader(BOOLEAN textureEnabled) : LightingShader(TRUE, textureEnabled, FALSE) {} };
#endif
