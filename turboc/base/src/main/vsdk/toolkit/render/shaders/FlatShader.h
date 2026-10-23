#ifndef __FLAT_SHADER__
#define __FLAT_SHADER__
#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/render/shaders/LightingShader.h"
class FlatShader : public LightingShader { public: FlatShader() : LightingShader(TRUE, FALSE, FALSE) {} };
#endif
