#ifndef __VSDK_TOOLKIT_RENDER_SHADERS_GOURAUDTEXTURESHADER_H__
#define __VSDK_TOOLKIT_RENDER_SHADERS_GOURAUDTEXTURESHADER_H__
#include "LightingShader.h"
class GouraudTextureShader : public LightingShader { public: explicit GouraudTextureShader(bool textureEnabled) : LightingShader(true, textureEnabled, false) {} };
#endif
