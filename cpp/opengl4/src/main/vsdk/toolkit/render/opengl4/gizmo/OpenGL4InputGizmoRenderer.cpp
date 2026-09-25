#include <algorithm>
#include <GL/glew.h>

#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/gui/gizmo/InputGizmo.h"
#include "vsdk/toolkit/gui/viewport/Viewport.h"
#include "vsdk/toolkit/gui/viewport/ViewportElementScaler.h"
#include "vsdk/toolkit/gui/viewport/ViewportSet.h"
#include "vsdk/toolkit/media/RGBAImageUncompressed.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4ColoredPrimitiveRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4ImageRenderer.h"
#include "vsdk/toolkit/render/opengl4/gizmo/OpenGL4InputGizmoRenderer.h"

namespace {
bool sameColor(const ColorRgb& a, const ColorRgb& b)
{
    return a.r() == b.r() && a.g() == b.g() && a.b() == b.b();
}
}

OpenGL4InputGizmoRenderer::OpenGL4InputGizmoRenderer(Host* host,
    ViewportSet* viewportSet, Viewport* viewport)
    : host(host), viewportSet(viewportSet), viewport(viewport),
      imageFontSize(0), referenceTextWidth(0)
{
}

OpenGL4InputGizmoRenderer::~OpenGL4InputGizmoRenderer()
{
    discardImages();
}

void OpenGL4InputGizmoRenderer::draw(InputGizmo* gizmo)
{
    if ( gizmo == nullptr ) {
        return;
    }
    int count = gizmo->getNumberOfFields();
    if ( count <= 0 ) {
        return;
    }
    ViewportElementScaler* scaler = viewportSet->getElementScaler();
    int fontSize = scaler->scaleSize(BASE_FONT_SIZE);
    int margin = scaler->scaleSize(BASE_MARGIN);
    int gap = scaler->scaleSize(BASE_GAP);
    int paddingX = scaler->scaleSize(BASE_PADDING_X);
    int paddingY = scaler->scaleSize(BASE_PADDING_Y);
    int frameThickness = std::max(1, scaler->scaleSize(BASE_FRAME_THICKNESS));
    int underlineDistance = scaler->scaleSize(BASE_UNDERLINE_DISTANCE);
    int underlineThickness =
        std::max(1, scaler->scaleSize(BASE_UNDERLINE_THICKNESS));
    int i;

    if ( images.size() != count ) {
        discardImages();
        images.clear();
        imageTexts.clear();
        imageColors.clear();
        for ( i = 0; i < count; i++ ) {
            images.add(nullptr);
            imageTexts.add(java::String(""));
            imageColors.add(ColorRgb());
        }
    }
    // The frames have at least the width of the reference text of the gizmo
    if ( fontSize != imageFontSize || referenceTextWidth == 0 ||
         !gizmo->getReferenceText().equals(referenceText) ) {
        // The reference image is only measured, it is never drawn
        referenceText = gizmo->getReferenceText();
        RGBAImageUncompressed* reference = host->createLabelImage(
            referenceText, InputGizmo::HIGHLIGHT_COLOR, fontSize);
        referenceTextWidth = reference != nullptr ? reference->getXSize() : 0;
        delete reference;
    }

    //-----------------------------------------------------------------
    java::ArrayList<ColorRgb> colors;
    java::ArrayList<int> frameWidths;
    int totalWidth = 0;

    for ( i = 0; i < count; i++ ) {
        colors.add(gizmo->getFieldDisplayColor(i));
        updateImage(i, gizmo->getDisplayText(i), colors.get(i), fontSize);
        if ( images.get(i) == nullptr ) {
            return;
        }
        frameWidths.add(std::max(referenceTextWidth,
            (int)images.get(i)->getXSize()) + 2*(paddingX + frameThickness));
        totalWidth += frameWidths.get(i);
    }
    imageFontSize = fontSize;
    totalWidth += (count - 1) * gap;

    int frameHeight = images.get(0)->getYSize() + 2*(paddingY + frameThickness);
    int x = viewport->getPixelSizeX() - margin - totalWidth;
    int y = margin;
    java::ArrayList<Rectangle> rectangles;
    java::ArrayList<int> frameStarts;

    for ( i = 0; i < count; i++ ) {
        frameStarts.add(x);
        addFrame(rectangles, x, y, frameWidths.get(i), frameHeight,
            frameThickness, colors.get(i));
        if ( i == gizmo->getSelectedField() ) {
            int underlineY = y - underlineDistance - underlineThickness;

            addRectangle(rectangles, x, underlineY, x + frameWidths.get(i),
                underlineY + underlineThickness, colors.get(i));
        }
        x += frameWidths.get(i) + gap;
    }
    drawRectangles(rectangles);

    for ( i = 0; i < count; i++ ) {
        int textX = frameStarts.get(i) +
            (frameWidths.get(i) - (int)images.get(i)->getXSize()) / 2;
        int textY = y + frameThickness + paddingY;

        host->drawLabelImage(images.get(i), textX, textY);
    }
}

void OpenGL4InputGizmoRenderer::disposeGlResources()
{
    for ( long i = 0; i < images.size(); i++ ) {
        if ( images.get(i) != nullptr ) {
            OpenGL4ImageRenderer::unload(images.get(i));
            delete images.get(i);
            images.set(i, nullptr);
            imageTexts.set(i, java::String(""));
        }
    }
}

void OpenGL4InputGizmoRenderer::discardImages()
{
    for ( long i = 0; i < images.size(); i++ ) {
        if ( images.get(i) != nullptr ) {
            host->discardLabelImage(images.get(i));
            images.set(i, nullptr);
        }
    }
}

void OpenGL4InputGizmoRenderer::updateImage(int field, const java::String& text,
    const ColorRgb& color, int fontSize)
{
    if ( images.get(field) != nullptr && fontSize == imageFontSize &&
         text.equals(imageTexts.get(field)) &&
         sameColor(color, imageColors.get(field)) ) {
        return;
    }
    if ( images.get(field) != nullptr ) {
        host->discardLabelImage(images.get(field));
    }
    // An empty text (i.e. all its characters deleted) cannot be rasterized:
    // an empty box is drawn with a blank text
    images.set(field, host->createLabelImage(
        text.isEmpty() ? java::String(" ") : text, color, fontSize));
    imageTexts.set(field, text);
    imageColors.set(field, color);
}

//= Frames ============================================================

void OpenGL4InputGizmoRenderer::addRectangle(
    java::ArrayList<Rectangle>& rectangles, int x0, int y0, int x1, int y1,
    const ColorRgb& c)
{
    Rectangle r = {{(float)x0, (float)y0, (float)x1, (float)y1,
                    (float)c.r(), (float)c.g(), (float)c.b()}};

    rectangles.add(r);
}

void OpenGL4InputGizmoRenderer::addFrame(java::ArrayList<Rectangle>& rectangles,
    int x, int y, int width, int height, int thickness, const ColorRgb& c)
{
    addRectangle(rectangles, x, y, x + width, y + thickness, c);
    addRectangle(rectangles, x, y + height - thickness, x + width, y + height, c);
    addRectangle(rectangles, x, y + thickness, x + thickness,
        y + height - thickness, c);
    addRectangle(rectangles, x + width - thickness, y + thickness, x + width,
        y + height - thickness, c);
}

void OpenGL4InputGizmoRenderer::drawRectangles(
    const java::ArrayList<Rectangle>& rectangles)
{
    if ( rectangles.size() == 0 ) {
        return;
    }
    GLint currentViewport[4];

    glGetIntegerv(GL_VIEWPORT, currentViewport);

    int surfaceWidth = viewportSet->getSizeXInPixels();
    int surfaceHeight = viewportSet->getSizeYInPixels();

    if ( surfaceWidth <= 0 || surfaceHeight <= 0 ) {
        surfaceWidth = currentViewport[0] + currentViewport[2];
        surfaceHeight = currentViewport[1] + currentViewport[3];
    }

    java::ArrayList<float> positions;
    java::ArrayList<float> colors;

    for ( long i = 0; i < rectangles.size(); i++ ) {
        const float* r = rectangles.get(i).values;
        float x0 = viewport->getPixelStartX() + r[0];
        float y0 = viewport->getPixelStartY() + r[1];
        float x1 = viewport->getPixelStartX() + r[2];
        float y1 = viewport->getPixelStartY() + r[3];
        float corners[] = {x0, y0,  x1, y0,  x1, y1,  x0, y0,  x1, y1,  x0, y1};

        for ( int k = 0; k < 6; k++ ) {
            positions.add(2.0f * corners[2*k] / surfaceWidth - 1.0f);
            positions.add(2.0f * corners[2*k + 1] / surfaceHeight - 1.0f);
            positions.add(0);
            colors.add(r[4]);
            colors.add(r[5]);
            colors.add(r[6]);
            colors.add(1.0f);
        }
    }

    GLboolean scissorWasEnabled = glIsEnabled(GL_SCISSOR_TEST);
    GLint previousScissor[4];

    glGetIntegerv(GL_SCISSOR_BOX, previousScissor);

    glViewport(0, 0, surfaceWidth, surfaceHeight);
    glEnable(GL_SCISSOR_TEST);
    glScissor(viewport->getPixelStartX(), viewport->getPixelStartY(),
        viewport->getPixelSizeX(), viewport->getPixelSizeY());
    glDisable(GL_DEPTH_TEST);
    glDisable(GL_CULL_FACE);
    glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);

    OpenGL4ColoredPrimitiveRenderer::draw(Matrix4x4d::identityMatrix(),
        GL_TRIANGLES, positions, colors);

    glEnable(GL_DEPTH_TEST);
    glScissor(previousScissor[0], previousScissor[1], previousScissor[2],
        previousScissor[3]);
    if ( !scissorWasEnabled ) {
        glDisable(GL_SCISSOR_TEST);
    }
    glViewport(currentViewport[0], currentViewport[1], currentViewport[2],
        currentViewport[3]);
}
