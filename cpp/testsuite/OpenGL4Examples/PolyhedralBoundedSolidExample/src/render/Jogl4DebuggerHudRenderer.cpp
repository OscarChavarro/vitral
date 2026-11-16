#include <cmath>

#include <glad/gl.h>

#include "java/util/ArrayList.txx"
#include "render/Jogl4DebuggerHudRenderer.h"

#include "java/lang/Math.h"
#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/common/statistics/PolyhedralBoundedSolidStatistics.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/media/RGBAImageUncompressed.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4LabelCanvas.h"
#include "vsdk/toolkit/render/opengl4/polyhedralBoundedSolid/OpenGL4PolyhedralBoundedSolidDebugHUDRenderer.h"
#include "models/DebuggerModel.h"

static const int LINE_HEIGHT = 34;
static const int HUD_TOP_PADDING = 28;
static const int HUD_LEFT_PADDING = 16;
static const int HUD_BOTTOM_PADDING = 16;

/// Java: `new Font("SansSerif", Font.BOLD, 18)`
static const int HUD_FONT_SIZE = 18;
/// Java: `new Font("SansSerif", Font.PLAIN, 12)`
static const int LABEL_FONT_SIZE = 12;

/// Java: `java.awt.Color` of 8 bit channels
static ColorRgb color255(int r, int g, int b)
{
    return ColorRgb(r / 255.0, g / 255.0, b / 255.0);
}

static java::String l2s(long long value)
{
    return java::String::valueOf(value);
}

Jogl4DebuggerHudRenderer::Jogl4DebuggerHudRenderer(DebuggerModel* model,
    OpenGL4LabelImageProvider* labels)
    : model(model), labels(labels), overlay(nullptr), viewportWidth(0),
      viewportHeight(0)
{
}

Jogl4DebuggerHudRenderer::~Jogl4DebuggerHudRenderer()
{
    delete overlay;
}

void Jogl4DebuggerHudRenderer::init(int surfaceWidth, int surfaceHeight)
{
    updateViewportSize(surfaceWidth, surfaceHeight);
}

void Jogl4DebuggerHudRenderer::updateViewportSize(int width, int height)
{
    viewportWidth = java::Math::max(1, width);
    viewportHeight = java::Math::max(1, height);
}

void Jogl4DebuggerHudRenderer::draw()
{
    if ( model == nullptr ) {
        return;
    }

    GLint viewport[4];
    glGetIntegerv(GL_VIEWPORT, viewport);
    int width = viewportWidth > 0 ? viewportWidth : java::Math::max(1, (int)viewport[2]);
    int height = viewportHeight > 0 ? viewportHeight : java::Math::max(1, (int)viewport[3]);
    updateViewportSize(width, height);
    ensureOverlayBuffers(width, height);

    OpenGL4LabelCanvas* g = overlay;
    g->clear(width, height);

    drawHudText(g, width, height);
    drawSelectedFaceLabel(g);
    drawDebugVertexLabels(g);

    // Java: `copyBufferedOverlayToImage`, `Jogl4ImageRenderer.unload` and
    // `Jogl4ImageRenderer.draw`
    g->draw();
}

void Jogl4DebuggerHudRenderer::dispose()
{
    if ( overlay != nullptr ) {
        overlay->unload();
    }
    delete overlay;
    overlay = nullptr;
}

void Jogl4DebuggerHudRenderer::drawHudText(OpenGL4LabelCanvas* g, int width,
    int height)
{
    int nextLeftLine = 2;
    int blockHeight = HUD_TOP_PADDING + 5 * LINE_HEIGHT + HUD_BOTTOM_PADDING;
    if ( SolidModelNamesUtil::usesCsgDebugControls(model->getSolidModelName()) ) {
        blockHeight += model->usesKurlanderBowlSingleMotifControls()
            ? 2 * LINE_HEIGHT
            : LINE_HEIGHT;
    }

    fillRect(g, 0, 0, width, java::Math::min(height, blockHeight), 0, 0, 0, 180);
    g->setFontSize(HUD_FONT_SIZE);
    g->setColor(color255(255, 242, 51));

    java::String showingFaceLoopMessage =
        java::String("Face [1, 2]: ") + formatFaceLoopLabel();
    java::String selectedModelMessage = java::String("Selected model [3, 4]: ")
        + SolidModelNamesUtil::name(model->getSolidModelName())
        + " (" + java::String::valueOf(
            SolidModelNamesUtil::getDisplayIndex(model->getSolidModelName()))
        + "/" + java::String::valueOf(SolidModelNamesUtil::getTotalModels()) + ")";
    java::String csgSampleMessage = java::String("CSG sample [6]: ")
        + CsgSampleNamesUtil::getLabel(model->getCsgSample())
        + " (" + java::String::valueOf(
            CsgSampleNamesUtil::getDisplayIndex(model->getCsgSample()))
        + "/" + java::String::valueOf(CsgSampleNamesUtil::getTotalSamples()) + ")";
    java::String kurlanderMotifMessage = java::String("Motif [e, E]: ")
        + model->getKurlanderBowlSingleMotifLabel();
    java::String csgOperationMessage = java::String("CSG op [5]: ")
        + CsgOperationNamesUtil::getLabel(model->getCsgOperation());
    java::String referenceFrameMessage = java::String("Reference frame [Space]: ")
        + (model->isShowCoordinateSystem() ? "ON" : "OFF");
    java::String nrMessage = java::String("NR [q, Q]: ") +
        java::String::valueOf(model->getSubdivisionCircumference());
    java::String nhMessage = java::String("NH [w, W]: ") +
        java::String::valueOf(model->getSubdivisionHeight());

    g->drawString(showingFaceLoopMessage, HUD_LEFT_PADDING, height - (height - HUD_TOP_PADDING));
    g->drawString(selectedModelMessage, HUD_LEFT_PADDING, HUD_TOP_PADDING + LINE_HEIGHT);

    if ( SolidModelNamesUtil::usesCsgDebugControls(model->getSolidModelName()) ) {
        g->drawString(csgSampleMessage, HUD_LEFT_PADDING,
            HUD_TOP_PADDING + nextLeftLine * LINE_HEIGHT);
        nextLeftLine++;
        if ( model->usesKurlanderBowlSingleMotifControls() ) {
            g->drawString(kurlanderMotifMessage, HUD_LEFT_PADDING,
                HUD_TOP_PADDING + nextLeftLine * LINE_HEIGHT);
            nextLeftLine++;
        }
        g->drawString(csgOperationMessage, HUD_LEFT_PADDING,
            HUD_TOP_PADDING + nextLeftLine * LINE_HEIGHT);
        nextLeftLine++;
    }
    g->drawString(referenceFrameMessage, HUD_LEFT_PADDING,
        HUD_TOP_PADDING + nextLeftLine * LINE_HEIGHT);
    nextLeftLine++;
    java::String hiddenLineMessage = java::String("Hidden line [8]: ")
        + AppelDisplayModeUtil::getLabel(model->getAppelDisplayMode());
    g->drawString(hiddenLineMessage, HUD_LEFT_PADDING,
        HUD_TOP_PADDING + nextLeftLine * LINE_HEIGHT);
    drawTopRight(g, width, nrMessage, HUD_TOP_PADDING);
    drawTopRight(g, width, nhMessage, HUD_TOP_PADDING + LINE_HEIGHT);

    if ( model->isErrorState() ) {
        g->setColor(color255(255, 38, 38));
        g->drawString(model->getErrorMessage(), HUD_LEFT_PADDING, height - 16);
        g->setColor(color255(255, 242, 51));
    }
    drawCsgStatisticsSummary(g, height);
}

void Jogl4DebuggerHudRenderer::drawCsgStatisticsSummary(OpenGL4LabelCanvas* g,
    int height)
{
    if ( !SolidModelNamesUtil::usesCsgDebugControls(model->getSolidModelName()) ) {
        return;
    }
    if ( !PolyhedralBoundedSolidStatistics::isEnabled() ) {
        return;
    }
    if ( PolyhedralBoundedSolidStatistics::getSetOpCalls() <= 0 ) {
        return;
    }

    long long failures = PolyhedralBoundedSolidStatistics::getOperationFailureCases();
    long long warnings = PolyhedralBoundedSolidStatistics::getConsistencyWarningCases();
    long long he1eqhe2 = PolyhedralBoundedSolidStatistics::getHe1EqualsHe2Cases();
    long long invalidInputs =
        PolyhedralBoundedSolidStatistics::getInvalidHalfEdgeInputCases();
    long long joinIncomplete =
        PolyhedralBoundedSolidStatistics::getJoinIncompleteCases();

    long long issueTotal = failures + warnings + he1eqhe2 +
        invalidInputs + joinIncomplete;
    if ( issueTotal <= 0 ) {
        return;
    }

    int lineGap = 22;
    int startY = model->isErrorState() ? (16 + (3 * lineGap)) : 16;

    g->setColor(color255(255, 38, 38));
    g->drawString("CSG stats issues:", HUD_LEFT_PADDING, height - startY - 2 * lineGap);
    g->drawString(
        java::String("fail=") + l2s(failures) + " warn=" + l2s(warnings) +
        " he1==he2=" + l2s(he1eqhe2),
        HUD_LEFT_PADDING, height - startY - lineGap);
    g->drawString(
        java::String("joinIncomplete=") + l2s(joinIncomplete) +
        " invalidHE=" + l2s(invalidInputs),
        HUD_LEFT_PADDING, height - startY);
}

void Jogl4DebuggerHudRenderer::drawDebugVertexLabels(OpenGL4LabelCanvas* g)
{
    PolyhedralBoundedSolid* solid = model->getSolid();
    if ( model->notDebugVertices() || solid == nullptr ) {
        return;
    }
    g->setFontSize(LABEL_FONT_SIZE);
    OpenGL4PolyhedralBoundedSolidDebugHUDRenderer::drawDebugVertexLabels(g,
        solid, model->getCamera(), viewportWidth, viewportHeight);
}

void Jogl4DebuggerHudRenderer::drawSelectedFaceLabel(OpenGL4LabelCanvas* g)
{
    g->setFontSize(LABEL_FONT_SIZE);
    OpenGL4PolyhedralBoundedSolidDebugHUDRenderer::drawSelectedFaceLabel(g,
        model->getSolid(), model->getFaceIndex(), model->getCamera(),
        viewportWidth, viewportHeight);
}

java::String Jogl4DebuggerHudRenderer::formatFaceLoopLabel() const
{
    if ( model->getFaceIndex() == -2 ) {
        return "NONE";
    }
    if ( model->getFaceIndex() == -1 ) {
        return "ALL";
    }

    int currentFace = model->getFaceIndex() + 1;
    int totalFaces = 0;
    if ( model->getSolid() != nullptr ) {
        totalFaces = (int)model->getSolid()->getPolygonsList().size();
    }
    return java::String("[") + java::String::valueOf(currentFace) + "/" +
        java::String::valueOf(totalFaces) + "]";
}

void Jogl4DebuggerHudRenderer::ensureOverlayBuffers(int width, int height)
{
    if ( overlay != nullptr && overlay->getWidth() == width &&
         overlay->getHeight() == height ) {
        return;
    }
    if ( overlay == nullptr ) {
        overlay = new OpenGL4LabelCanvas(labels, width, height);
    }
    else {
        overlay->clear(width, height);
    }
}

/**
Java: `Graphics2D.fillRect` with a translucent color over the cleared
(transparent) overlay; rows are counted from the top.
*/
void Jogl4DebuggerHudRenderer::fillRect(OpenGL4LabelCanvas* g, int x, int y,
    int width, int height, int r, int gr, int b, int a)
{
    RGBAImageUncompressed* image = g->getImage();
    int x0 = java::Math::max(0, x);
    int y0 = java::Math::max(0, y);
    int x1 = java::Math::min(image->getXSize(), x + width);
    int y1 = java::Math::min(image->getYSize(), y + height);
    int i;
    int j;
    for ( j = y0; j < y1; j++ ) {
        for ( i = x0; i < x1; i++ ) {
            image->putPixel(i, j, (char)r, (char)gr, (char)b, (char)a);
        }
    }
}

void Jogl4DebuggerHudRenderer::drawTopRight(OpenGL4LabelCanvas* g, int width,
    const java::String& text, int baselineY)
{
    int textWidth = g->stringWidth(text);
    int x = width - HUD_LEFT_PADDING - textWidth;
    g->drawString(text, java::Math::max(HUD_LEFT_PADDING, x), baselineY);
}
