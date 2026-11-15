#ifndef __OPN_GL_1_LBL_IMG_PRVDR__
#define __OPN_GL_1_LBL_IMG_PRVDR__
#include "java/lang/String.h"
class ColorRgb; class RGBAImageUncompressed;
class OpenGL1LabelImageProvider { public: enum { DEFAULT_FONT_SIZE = 12 }; virtual ~OpenGL1LabelImageProvider(){}; virtual RGBAImageUncompressed* createLabelImage(const String&,const ColorRgb&,int)=0; };
#endif
