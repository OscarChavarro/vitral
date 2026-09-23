#include "render/opengl4/OpenGL4FrameBufferSource.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4FrameBufferReader.h"
RGBImageUncompressed*OpenGL4FrameBufferSource::readColor(){return OpenGL4FrameBufferReader::readColor();}ZBuffer*OpenGL4FrameBufferSource::readDepth(){return OpenGL4FrameBufferReader::readDepth();}
