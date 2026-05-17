#ifndef __VSDK_TOOLKIT_RENDER_SHADERS_FLATTEXTUREDSHADER_H__
#define __VSDK_TOOLKIT_RENDER_SHADERS_FLATTEXTUREDSHADER_H__
#include "LightingShader.h"
class FlatTexturedShader : public LightingShader { public: FlatTexturedShader() : LightingShader(true, true, false) {} };
#endif
