#include "OpenGlOfflineSphereRenderer.h"
#include "../model/ShadersModel.h"

#include <vector>

#include <GL/glew.h>
#define GLFW_INCLUDE_NONE
#include <GLFW/glfw3.h>

#include "vsdk/toolkit/media/RGBImageUncompressed.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4SphereRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4ImageRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4MatrixRenderer.h"

using vsdk::toolkit::render::opengl4::OpenGL4SphereRenderer;
using vsdk::toolkit::render::opengl4::OpenGL4MatrixRenderer;

RGBImageUncompressed* OpenGlOfflineSphereRenderer::render(
    ShadersModel* model,
    const Matrix4x4d& modelRotation,
    int width,
    int height)
{
    if (!model) return 0;
    if (!glfwInit()) return 0;
    glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
    glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
    glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 1);
    glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
#ifdef __APPLE__
    glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
#endif
    GLFWwindow* window = glfwCreateWindow(width, height, "offline", 0, 0);
    if (!window) { glfwTerminate(); return 0; }
    glfwMakeContextCurrent(window);
    glewExperimental = GL_TRUE;
    if (glewInit() != GLEW_OK) { glfwDestroyWindow(window); glfwTerminate(); return 0; }

    model->camera->updateViewportResize(width, height);
    glViewport(0, 0, width, height);
    glEnable(GL_DEPTH_TEST);
    glClearColor(0, 0, 0, 1);
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

    OpenGL4SphereRenderer::draw(
        model->sphere,
        model->camera,
        model->light,
        &model->material,
        &model->quality,
        model->textureMap,
        model->bumpMapHeightRgb,
        modelRotation,
        model->sphereMeridians,
        model->sphereParallels);

    glFinish();

    std::vector<unsigned char> bytes((size_t)width * (size_t)height * 3u, 0u);
    glPixelStorei(GL_PACK_ALIGNMENT, 1);
    glReadPixels(0, 0, width, height, GL_RGB, GL_UNSIGNED_BYTE, bytes.data());

    RGBImageUncompressed* image = new RGBImageUncompressed();
    image->init(width, height);
    size_t pos = 0;
    for (int y = height - 1; y >= 0; y--) {
        for (int x = 0; x < width; x++) {
            image->putPixel(x, y, (char)bytes[pos], (char)bytes[pos + 1], (char)bytes[pos + 2]);
            pos += 3;
        }
    }

    OpenGL4SphereRenderer::dispose();
    OpenGL4ImageRenderer::dispose();
    OpenGL4MatrixRenderer::release();
    glfwDestroyWindow(window);
    glfwTerminate();
    return image;
}
