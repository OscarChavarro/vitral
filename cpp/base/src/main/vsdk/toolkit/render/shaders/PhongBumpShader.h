#ifndef __VSDK_TOOLKIT_RENDER_SHADERS_PHONGBUMPSHADER_H__
#define __VSDK_TOOLKIT_RENDER_SHADERS_PHONGBUMPSHADER_H__
#include "LightingShader.h"
class PhongBumpShader : public LightingShader { public: PhongBumpShader() : LightingShader(true, false, true) {} };
#endif
