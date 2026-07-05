#ifndef __GOURAUDTEXTURESHADER__
#define __GOURAUDTEXTURESHADER__
#include "vsdk/toolkit/render/shaders/LightingShader.h"
class GouraudTextureShader : public LightingShader { public: explicit GouraudTextureShader(bool textureEnabled) : LightingShader(true, textureEnabled, false) {} };
#endif
