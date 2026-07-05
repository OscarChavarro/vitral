#ifndef __PHONGBUMPSHADER__
#define __PHONGBUMPSHADER__
#include "vsdk/toolkit/render/shaders/LightingShader.h"
class PhongBumpShader : public LightingShader { public: PhongBumpShader() : LightingShader(true, false, true) {} };
#endif
