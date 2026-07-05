#ifndef __FLATSHADER__
#define __FLATSHADER__
#include "vsdk/toolkit/render/shaders/LightingShader.h"
class FlatShader : public LightingShader { public: FlatShader() : LightingShader(true, false, false) {} };
#endif
