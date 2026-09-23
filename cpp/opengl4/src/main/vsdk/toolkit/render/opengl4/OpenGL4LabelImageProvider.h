#ifndef __OPEN_GL_4_LABEL_IMAGE_PROVIDER__
#define __OPEN_GL_4_LABEL_IMAGE_PROVIDER__
#include "java/lang/String.h"
class ColorRgb; class RGBAImageUncompressed;
class OpenGL4LabelImageProvider { public: static const int DEFAULT_FONT_SIZE=12; virtual ~OpenGL4LabelImageProvider(){}; virtual RGBAImageUncompressed* createLabelImage(const java::String&,const ColorRgb&,int)=0; };
#endif
