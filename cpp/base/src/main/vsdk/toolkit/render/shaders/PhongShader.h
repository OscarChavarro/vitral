#ifndef __VSDK_TOOLKIT_RENDER_SHADERS_PHONGSHADER_H__
#define __VSDK_TOOLKIT_RENDER_SHADERS_PHONGSHADER_H__
#include "LightingShader.h"
class PhongShader : public LightingShader { public: PhongShader() : LightingShader(true, false, false) {} };
#endif
