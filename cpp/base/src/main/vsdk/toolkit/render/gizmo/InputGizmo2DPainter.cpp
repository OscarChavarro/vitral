#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/gui/gizmo/InputGizmo.h"
#include "vsdk/toolkit/gui/viewport/ViewportElementScaler.h"
#include "vsdk/toolkit/render/gizmo/Gizmo2DDrawList.h"
#include "vsdk/toolkit/render/gizmo/InputGizmo2DPainter.h"

namespace {
// Sizes, in pixels, designed for legacy resolutions
const int BASE_FONT_SIZE = 14;
const int BASE_MARGIN = 10;
const int BASE_GAP = 4;
const int BASE_PADDING_X = 3;
const int BASE_PADDING_Y = 1;
const int BASE_FRAME_THICKNESS = 1;
const int BASE_UNDERLINE_DISTANCE = 2;
const int BASE_UNDERLINE_THICKNESS = 2;

int maximum(int a, int b)
{
    return a > b ? a : b;
}
}

InputGizmo2DPainter::InputGizmo2DPainter()
{
}

void InputGizmo2DPainter::paint(Gizmo2DDrawList* out, const InputGizmo* gizmo,
    int viewportWidth, int viewportHeight,
    const ViewportElementScaler* scaler, const TextMeasurer& measurer)
{
    if ( out == nullptr || gizmo == nullptr ||
         gizmo->getNumberOfFields() <= 0 ) {
        return;
    }
    int count = gizmo->getNumberOfFields();
    int fontSize = scaler->scaleSize(BASE_FONT_SIZE);
    int margin = scaler->scaleSize(BASE_MARGIN);
    int gap = scaler->scaleSize(BASE_GAP);
    int paddingX = scaler->scaleSize(BASE_PADDING_X);
    int paddingY = scaler->scaleSize(BASE_PADDING_Y);
    int frameThickness = maximum(1, scaler->scaleSize(BASE_FRAME_THICKNESS));
    int underlineDistance = scaler->scaleSize(BASE_UNDERLINE_DISTANCE);
    int underlineThickness =
        maximum(1, scaler->scaleSize(BASE_UNDERLINE_THICKNESS));
    int referenceWidth =
        measurer.getTextWidth(gizmo->getReferenceText(), fontSize);
    int textHeight = measurer.getTextHeight(fontSize);

    //---------------------------------------------------------------------
    java::ArrayList<ColorRgb> colors;
    java::ArrayList<java::String> texts;
    java::ArrayList<int> textWidths;
    java::ArrayList<int> frameWidths;
    int totalWidth = (count - 1) * gap;
    int i;

    for ( i = 0; i < count; i++ ) {
        colors.add(gizmo->getFieldDisplayColor(i));
        // An empty text (i.e. all its characters deleted) is a blank box
        java::String text = gizmo->getDisplayText(i);
        if ( text.isEmpty() ) {
            text = java::String(" ");
        }
        texts.add(text);
        textWidths.add(measurer.getTextWidth(texts[i], fontSize));
        frameWidths.add(maximum(referenceWidth, textWidths[i]) +
            2*(paddingX + frameThickness));
        totalWidth += frameWidths[i];
    }

    int frameHeight = textHeight + 2*(paddingY + frameThickness);
    int x = viewportWidth - margin - totalWidth;
    int top = viewportHeight - margin - frameHeight;

    for ( i = 0; i < count; i++ ) {
        addFrame(out, x, top, frameWidths[i], frameHeight, frameThickness,
                 colors[i]);
        if ( i == gizmo->getSelectedField() ) {
            out->addOverlayRectangle(x, top + frameHeight + underlineDistance,
                frameWidths[i], underlineThickness, colors[i]);
        }
        out->addOverlayLabel(x + (frameWidths[i] - textWidths[i]) / 2,
            top + frameHeight - frameThickness - paddingY, texts[i],
            colors[i], fontSize, Gizmo2DDrawList::Alignment::LOWER_LEFT);
        x += frameWidths[i] + gap;
    }
}

void InputGizmo2DPainter::addFrame(Gizmo2DDrawList* out, int x, int y,
    int width, int height, int thickness, const ColorRgb& c)
{
    out->addOverlayRectangle(x, y, width, thickness, c);
    out->addOverlayRectangle(x, y + height - thickness, width, thickness, c);
    out->addOverlayRectangle(x, y + thickness, thickness,
        height - 2*thickness, c);
    out->addOverlayRectangle(x + width - thickness, y + thickness, thickness,
        height - 2*thickness, c);
}
