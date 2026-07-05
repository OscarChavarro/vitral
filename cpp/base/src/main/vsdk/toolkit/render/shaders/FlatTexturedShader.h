#ifndef __FLATTEXTUREDSHADER__
#define __FLATTEXTUREDSHADER__
#include "vsdk/toolkit/render/shaders/LightingShader.h"
class FlatTexturedShader : public LightingShader { public: FlatTexturedShader() : LightingShader(true, true, false) {} };
#endif
