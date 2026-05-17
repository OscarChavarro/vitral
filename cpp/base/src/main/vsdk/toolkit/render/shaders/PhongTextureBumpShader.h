#ifndef __VSDK_TOOLKIT_RENDER_SHADERS_PHONGTEXTUREBUMPSHADER_H__
#define __VSDK_TOOLKIT_RENDER_SHADERS_PHONGTEXTUREBUMPSHADER_H__
#include "LightingShader.h"
class PhongTextureBumpShader : public LightingShader { public: PhongTextureBumpShader() : LightingShader(true, true, true) {} };
#endif
