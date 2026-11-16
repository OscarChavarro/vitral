#include <glad/gl.h>
#include "vsdk/toolkit/render/opengl4/OpenGL4Loader.h"
#define GLFW_INCLUDE_NONE
#include <GLFW/glfw3.h>

#include "render/Jogl4HeadlessRenderer.h"

#include "java/io/File.h"
#include "java/lang/IllegalStateException.h"
#include "java/lang/System.h"
#include "vsdk/toolkit/io/image/ImagePersistence.h"
#include "vsdk/toolkit/media/RGBImageUncompressed.h"

Jogl4HeadlessRenderer::Jogl4HeadlessRenderer(DebuggerModel* model,
    const java::String& outputFile)
    : model(model), outputFile(outputFile), width(1024), height(768),
      delegate(model, &labels), done(false)
{
}

Jogl4HeadlessRenderer::Jogl4HeadlessRenderer(DebuggerModel* model,
    const java::String& outputFile, int width, int height)
    : model(model), outputFile(outputFile), width(width), height(height),
      delegate(model, &labels), done(false)
{
}

void Jogl4HeadlessRenderer::render()
{
    if ( !glfwInit() ) {
        throw java::IllegalStateException("Failed to initialize GLFW");
    }
    glfwDefaultWindowHints();
    glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
    glfwWindowHint(GLFW_DOUBLEBUFFER, GLFW_FALSE);
    glfwWindowHint(GLFW_DEPTH_BITS, 24);
    glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
    glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 1);
    glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
    glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
    GLFWwindow* pbuffer = glfwCreateWindow(width, height, "offline", 0, 0);
    if ( pbuffer == 0 ) {
        glfwTerminate();
        throw java::IllegalStateException("Failed to create offscreen GLFW window");
    }
    glfwMakeContextCurrent(pbuffer);
    if ( !OpenGL4Loader::load(glfwGetProcAddress) ) {
        glfwDestroyWindow(pbuffer);
        glfwTerminate();
        throw java::IllegalStateException("Failed to load OpenGL 4 functions");
    }

    // The hidden window only gives the context: an unmapped window owns no
    // pixels on some drivers (its reads give the screen under it), so the
    // frame is drawn into a framebuffer object, as JOGL's pbuffer
    GLuint framebuffer = 0;
    GLuint colorBuffer = 0;
    GLuint depthBuffer = 0;
    glGenFramebuffers(1, &framebuffer);
    glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
    glGenRenderbuffers(1, &colorBuffer);
    glBindRenderbuffer(GL_RENDERBUFFER, colorBuffer);
    glRenderbufferStorage(GL_RENDERBUFFER, GL_RGBA8, width, height);
    glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0,
        GL_RENDERBUFFER, colorBuffer);
    glGenRenderbuffers(1, &depthBuffer);
    glBindRenderbuffer(GL_RENDERBUFFER, depthBuffer);
    glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT24, width, height);
    glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT,
        GL_RENDERBUFFER, depthBuffer);
    bool complete =
        glCheckFramebufferStatus(GL_FRAMEBUFFER) == GL_FRAMEBUFFER_COMPLETE;

    if ( complete ) {
        delegate.init(width, height);
        delegate.reshape(0, 0, width, height);
        display();
        // Java: `pbuffer.destroy()`, which calls `dispose`
        delegate.dispose();
    }

    glBindFramebuffer(GL_FRAMEBUFFER, 0);
    glDeleteRenderbuffers(1, &depthBuffer);
    glDeleteRenderbuffers(1, &colorBuffer);
    glDeleteFramebuffers(1, &framebuffer);
    glfwDestroyWindow(pbuffer);
    glfwTerminate();
    if ( !complete ) {
        throw java::IllegalStateException("Failed to create offscreen framebuffer");
    }
}

void Jogl4HeadlessRenderer::display()
{
    if ( done ) {
        return;
    }
    delegate.display(width, height);
    glFinish();

    RGBImageUncompressed* image =
        Jogl4DebuggerRenderer::captureRgbImage(width, height);
    Jogl4DebuggerRenderer::ensureParentFolder(outputFile);
    ImagePersistence::exportPNG(java::File(outputFile), image);
    delete image;
    java::System::out.println((java::String("[PolyhedralBoundedSolidExample] Exported ") +
        outputFile).c_str());

    done = true;
}
