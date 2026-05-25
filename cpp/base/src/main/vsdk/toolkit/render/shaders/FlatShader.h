#ifndef __VSDK_TOOLKIT_RENDER_SHADERS_FLATSHADER_H__
#define __VSDK_TOOLKIT_RENDER_SHADERS_FLATSHADER_H__
#include "vsdk/toolkit/render/shaders/LightingShader.h"
class FlatShader : public LightingShader { public: FlatShader() : LightingShader(true, false, false) {} };
#endif
