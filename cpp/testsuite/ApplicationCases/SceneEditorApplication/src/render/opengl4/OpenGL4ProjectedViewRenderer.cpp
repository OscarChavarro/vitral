#include <GL/glew.h>
#include "render/opengl4/OpenGL4ProjectedViewRenderer.h"
#include "render/opengl4/OpenGL4SceneRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4FrameBufferReader.h"
ZBuffer*OpenGL4ProjectedViewRenderer::renderDepth(SimpleBodyGroup*b,Camera*c,RendererConfiguration*q,int w,int h){glViewport(0,0,w,h);glClearColor(.5f,.5f,.9f,1);glClear(GL_COLOR_BUFFER_BIT|GL_DEPTH_BUFFER_BIT);glEnable(GL_DEPTH_TEST);glDepthMask(GL_TRUE);if(b)OpenGL4SceneRenderer::drawBodyGroup(b,c,0,q);glFlush();return OpenGL4FrameBufferReader::readDepth();}
