#include <cmath>

#include "vsdk/toolkit/media/RGBAImageUncompressed.h"
#include "vsdk/toolkit/media/RGBAPixel.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4ImageRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4LabelCanvas.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4LabelImageProvider.h"

namespace {
/// Margin the label providers leave around the text
const int LABEL_MARGIN = 2;
/// Ascent of the text over its baseline, as a fraction of the font size
const double ASCENT_FRACTION = 0.8;
}

OpenGL4LabelCanvas::OpenGL4LabelCanvas(OpenGL4LabelImageProvider* labels,
                                       int width, int height)
    : labels(labels), image(new RGBAImageUncompressed()), color(1, 1, 1),
      fontSize(OpenGL4LabelImageProvider::DEFAULT_FONT_SIZE)
{
    clear(width, height);
}

OpenGL4LabelCanvas::~OpenGL4LabelCanvas()
{
    delete image;
}

void OpenGL4LabelCanvas::clear(int width, int height)
{
    if ( width < 1 ) {
        width = 1;
    }
    if ( height < 1 ) {
        height = 1;
    }
    if ( image->getXSize() != width || image->getYSize() != height ) {
        OpenGL4ImageRenderer::unload(image);
    }
    // `init` leaves every pixel transparent black
    image->init(width, height);
}

int OpenGL4LabelCanvas::getWidth() const
{
    return image->getXSize();
}

int OpenGL4LabelCanvas::getHeight() const
{
    return image->getYSize();
}

void OpenGL4LabelCanvas::setColor(const ColorRgb& color)
{
    this->color = color;
}

void OpenGL4LabelCanvas::setFontSize(int fontSize)
{
    this->fontSize = fontSize;
}

int OpenGL4LabelCanvas::getFontSize() const
{
    return fontSize;
}

void OpenGL4LabelCanvas::drawString(const java::String& text, int x, int y)
{
    if ( labels == nullptr || text.isEmpty() ) {
        return;
    }
    RGBAImageUncompressed* label = labels->createLabelImage(text, color,
                                                            fontSize);
    if ( label == nullptr ) {
        return;
    }
    int width = image->getXSize();
    int height = image->getYSize();
    int left = x - LABEL_MARGIN;
    int top = y - (int)std::lround(fontSize * ASCENT_FRACTION) - LABEL_MARGIN;
    RGBAPixel source;
    RGBAPixel target;

    // Pixel rows of both images are counted from the top, as in Graphics2D
    for ( int row = 0; row < label->getYSize(); row++ ) {
        int screenRow = top + row;
        if ( screenRow < 0 || screenRow >= height ) {
            continue;
        }
        for ( int column = 0; column < label->getXSize(); column++ ) {
            int screenColumn = left + column;
            if ( screenColumn < 0 || screenColumn >= width ) {
                continue;
            }
            label->getPixelRgba(column, row, &source);
            if ( source.a == 0 ) {
                continue;
            }
            image->getPixelRgba(screenColumn, screenRow, &target);
            // Source over destination, with non premultiplied colors
            double a = (unsigned char)source.a / 255.0;
            double da = (unsigned char)target.a / 255.0;
            double outA = a + da * (1.0 - a);
            const char s[3] = { source.r, source.g, source.b };
            const char d[3] = { target.r, target.g, target.b };
            char result[3];
            for ( int c = 0; c < 3; c++ ) {
                double value = ((unsigned char)s[c] * a +
                    (unsigned char)d[c] * da * (1.0 - a)) / outA;
                result[c] = (char)(unsigned char)std::lround(value);
            }
            image->putPixel(screenColumn, screenRow, result[0], result[1],
                result[2], (char)(unsigned char)std::lround(outA * 255.0));
        }
    }
    delete label;
}

int OpenGL4LabelCanvas::stringWidth(const java::String& text)
{
    if ( labels == nullptr || text.isEmpty() ) {
        return 0;
    }
    RGBAImageUncompressed* label = labels->createLabelImage(text, color,
                                                            fontSize);
    if ( label == nullptr ) {
        return 0;
    }
    int width = label->getXSize() - 2 * LABEL_MARGIN;
    delete label;
    return width > 0 ? width : 0;
}

RGBAImageUncompressed* OpenGL4LabelCanvas::getImage() const
{
    return image;
}

void OpenGL4LabelCanvas::draw()
{
    OpenGL4ImageRenderer::unload(image);
    OpenGL4ImageRenderer::draw(image);
}

void OpenGL4LabelCanvas::unload()
{
    OpenGL4ImageRenderer::unload(image);
}
