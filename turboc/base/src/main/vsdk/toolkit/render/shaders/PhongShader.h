#ifndef __PHONG_SHADER__
#define __PHONG_SHADER__
#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/render/shaders/LightingShader.h"
class PhongShader : public LightingShader { public: PhongShader() : LightingShader(TRUE, FALSE, FALSE) {} };
#endif
