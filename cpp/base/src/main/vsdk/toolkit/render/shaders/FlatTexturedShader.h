#ifndef __VSDK_TOOLKIT_RENDER_SHADERS_FLATTEXTUREDSHADER_H__
#define __VSDK_TOOLKIT_RENDER_SHADERS_FLATTEXTUREDSHADER_H__
#include "vsdk/toolkit/render/shaders/LightingShader.h"
class FlatTexturedShader : public LightingShader { public: FlatTexturedShader() : LightingShader(true, true, false) {} };
#endif
