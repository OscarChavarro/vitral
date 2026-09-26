#include <cstdio>

#include "vsdk/toolkit/common/logging/Logger.h"
#include "vsdk/toolkit/media/RGBImageUncompressed.h"
#include "vsdk/toolkit/media/ZBuffer.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1ColorDepthImageRenderer.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1ImageRenderer.h"

float* OpenGL1ColorDepthImageRenderer::depthUploadBuffer = 0;
int OpenGL1ColorDepthImageRenderer::depthUploadCapacity = 0;

void OpenGL1ColorDepthImageRenderer::draw(RGBImageUncompressed* image, ZBuffer* depth)
{
    if ( image == 0 || image->getXSize() <= 0 || image->getYSize() <= 0 ) {
        return;
    }
    if ( depth != 0 && (depth->getXSize() != image->getXSize() ||
                        depth->getYSize() != image->getYSize()) ) {
        Logger::reportMessage("OpenGL1ColorDepthImageRenderer", Logger::WARNING, "draw",
            "Depth buffer size does not match the image size, drawing color only");
        depth = 0;
    }
    if ( depth == 0 ) {
        OpenGL1ImageRenderer::unload(image);
        OpenGL1ImageRenderer::draw(image);
        return;
    }

    flipDepthRows(depth);

    glPushAttrib(GL_ENABLE_BIT | GL_CURRENT_BIT);
    glDisable(GL_LIGHTING);
    glDisable(GL_TEXTURE_2D);
    glDisable(GL_CULL_FACE);
    moveRasterToLowerLeftCorner();
    glPixelStorei(GL_UNPACK_ALIGNMENT, 1);

    // The raw image stores its bottom row first, as glDrawPixels expects
    glDisable(GL_DEPTH_TEST);
    glDrawPixels(image->getXSize(), image->getYSize(), GL_RGB,
        GL_UNSIGNED_BYTE, image->getRawImage());

    // Depth writes need the depth test enabled; ALWAYS replaces what was
    // there. The color buffer is left untouched.
    glEnable(GL_DEPTH_TEST);
    glDepthFunc(GL_ALWAYS);
    glDepthMask(GL_TRUE);
    glColorMask(GL_FALSE, GL_FALSE, GL_FALSE, GL_FALSE);
    glDrawPixels(image->getXSize(), image->getYSize(), GL_DEPTH_COMPONENT,
        GL_FLOAT, depthUploadBuffer);
    glColorMask(GL_TRUE, GL_TRUE, GL_TRUE, GL_TRUE);

    glPopAttrib();
    glEnable(GL_DEPTH_TEST);
    glDepthFunc(GL_LESS);
}

void OpenGL1ColorDepthImageRenderer::moveRasterToLowerLeftCorner()
{
    glMatrixMode(GL_PROJECTION);
    glPushMatrix();
    glLoadIdentity();
    glMatrixMode(GL_MODELVIEW);
    glPushMatrix();
    glLoadIdentity();
    glRasterPos2f(-1.0f, -1.0f);
    glMatrixMode(GL_PROJECTION);
    glPopMatrix();
    glMatrixMode(GL_MODELVIEW);
    glPopMatrix();
}

void OpenGL1ColorDepthImageRenderer::flipDepthRows(ZBuffer* depth)
{
    int xSize = depth->getXSize();
    int ySize = depth->getYSize();

    // The depth buffer stores its top row first, while glDrawPixels expects
    // the bottom row first: depth rows are flipped
    int count = xSize * ySize;
    if ( depthUploadBuffer == 0 || depthUploadCapacity < count ) {
        delete[] depthUploadBuffer;
        depthUploadBuffer = new float[count];
        depthUploadCapacity = count;
    }
    const float* values = depth->getZBuffer();
    int write = 0;
    for ( int row = ySize - 1; row >= 0; row-- ) {
        for ( int x = 0; x < xSize; x++ ) {
            depthUploadBuffer[write++] = values[row * xSize + x];
        }
    }
}

void OpenGL1ColorDepthImageRenderer::dispose()
{
    delete[] depthUploadBuffer;
    depthUploadBuffer = 0;
    depthUploadCapacity = 0;
}
