#ifndef __PHONGSHADER__
#define __PHONGSHADER__
#include "vsdk/toolkit/render/shaders/LightingShader.h"
class PhongShader : public LightingShader { public: PhongShader() : LightingShader(true, false, false) {} };
#endif
