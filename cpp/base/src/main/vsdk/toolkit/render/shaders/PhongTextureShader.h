#ifndef __PHONGTEXTURESHADER__
#define __PHONGTEXTURESHADER__
#include "vsdk/toolkit/render/shaders/LightingShader.h"
class PhongTextureShader : public LightingShader { public: PhongTextureShader() : LightingShader(true, true, false) {} };
#endif
