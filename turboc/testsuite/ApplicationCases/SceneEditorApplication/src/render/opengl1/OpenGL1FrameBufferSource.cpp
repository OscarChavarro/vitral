#include "render/opengl1/OpenGL1FrameBufferSource.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1FrameBufferReader.h"
RGBImageUncompressed*OpenGL1FrameBufferSource::readColor(){return OpenGL1FrameBufferReader::readColor();}ZBuffer*OpenGL1FrameBufferSource::readDepth(){return OpenGL1FrameBufferReader::readDepth();}
