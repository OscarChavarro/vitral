#include "vsdk/toolkit/render/opengl1/OpenGL1Api.h"
#include "render/opengl1/OpenGL1ProjectedViewRenderer.h"
#include "render/opengl1/OpenGL1SceneRenderer.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1FrameBufferReader.h"
ZBuffer*OpenGL1ProjectedViewRenderer::renderDepth(SimpleBodyGroup*b,Camera*c,RendererConfiguration*q,int w,int h){glViewport(0,0,w,h);glClearColor(.5f,.5f,.9f,1);glClear(GL_COLOR_BUFFER_BIT|GL_DEPTH_BUFFER_BIT);glEnable(GL_DEPTH_TEST);glDepthMask(GL_TRUE);if(b)OpenGL1SceneRenderer::drawBodyGroup(b,c,0,q);glFlush();return OpenGL1FrameBufferReader::readDepth();}
