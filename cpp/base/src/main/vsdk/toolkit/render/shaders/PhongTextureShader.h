#ifndef __VSDK_TOOLKIT_RENDER_SHADERS_PHONGTEXTURESHADER_H__
#define __VSDK_TOOLKIT_RENDER_SHADERS_PHONGTEXTURESHADER_H__
#include "vsdk/toolkit/render/shaders/LightingShader.h"
class PhongTextureShader : public LightingShader { public: PhongTextureShader() : LightingShader(true, true, false) {} };
#endif
