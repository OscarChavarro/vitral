#ifndef __PHONGTEXTUREBUMPSHADER__
#define __PHONGTEXTUREBUMPSHADER__
#include "vsdk/toolkit/render/shaders/LightingShader.h"
class PhongTextureBumpShader : public LightingShader { public: PhongTextureBumpShader() : LightingShader(true, true, true) {} };
#endif
