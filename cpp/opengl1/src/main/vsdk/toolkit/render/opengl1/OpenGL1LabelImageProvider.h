#ifndef __OPEN_GL_1_LABEL_IMAGE_PROVIDER__
#define __OPEN_GL_1_LABEL_IMAGE_PROVIDER__
#include "java/lang/String.h"
class ColorRgb; class RGBAImageUncompressed;
class OpenGL1LabelImageProvider { public: static const int DEFAULT_FONT_SIZE=12; virtual ~OpenGL1LabelImageProvider(){}; virtual RGBAImageUncompressed* createLabelImage(const java::String&,const ColorRgb&,int)=0; };
#endif
