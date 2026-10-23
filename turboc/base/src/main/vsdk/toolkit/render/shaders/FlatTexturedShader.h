#ifndef __FLAT_TEXTURED_SHADER__
#define __FLAT_TEXTURED_SHADER__
#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/render/shaders/LightingShader.h"
class FlatTexturedShader : public LightingShader { public: FlatTexturedShader() : LightingShader(TRUE, TRUE, FALSE) {} };
#endif
