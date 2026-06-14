#include "render/CairoPdfCalligraphic2DBufferRenderer.h"

#include <algorithm>

#include "processing/StyledCalligraphic2DBuffer.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/media/Calligraphic2DBuffer.h"

void CairoPdfCalligraphic2DBufferRenderer::updateBounds(
    const Calligraphic2DBuffer& icon,
    double& minX,
    double& maxX,
    double& minY,
    double& maxY,
    bool& initialized) const
{
    for (int i = 0; i < icon.getNumLines(); ++i) {
        Vector3Dd* p0 = icon.get2DLinePoint0(i);
        Vector3Dd* p1 = icon.get2DLinePoint1(i);
        if (p0 == nullptr || p1 == nullptr) {
            delete p0;
            delete p1;
            continue;
        }

        const double xs[2] = { p0->x(), p1->x() };
        const double ys[2] = { p0->y(), p1->y() };
        for (int k = 0; k < 2; ++k) {
            if (!initialized) {
                minX = maxX = xs[k];
                minY = maxY = ys[k];
                initialized = true;
            }
            else {
                minX = std::min(minX, xs[k]);
                maxX = std::max(maxX, xs[k]);
                minY = std::min(minY, ys[k]);
                maxY = std::max(maxY, ys[k]);
            }
        }

        delete p0;
        delete p1;
    }
}

bool CairoPdfCalligraphic2DBufferRenderer::drawBuffer(
    cairo_t* cr,
    const Calligraphic2DBuffer& icon,
    double minX,
    double minY,
    double scale,
    double offsetX,
    double offsetY) const
{
    bool hasVisibleSegments = false;

    for (int i = 0; i < icon.getNumLines(); ++i) {
        Vector3Dd* p0 = icon.get2DLinePoint0(i);
        Vector3Dd* p1 = icon.get2DLinePoint1(i);
        if (p0 == nullptr || p1 == nullptr) {
            delete p0;
            delete p1;
            continue;
        }

        const double x0Mm = offsetX + (p0->x() - minX) * scale;
        const double y0Mm = offsetY + (p0->y() - minY) * scale;
        const double x1Mm = offsetX + (p1->x() - minX) * scale;
        const double y1Mm = offsetY + (p1->y() - minY) * scale;

        cairo_move_to(cr, x0Mm * 72.0 / 25.4, y0Mm * 72.0 / 25.4);
        cairo_line_to(cr, x1Mm * 72.0 / 25.4, y1Mm * 72.0 / 25.4);
        hasVisibleSegments = true;

        delete p0;
        delete p1;
    }

    if (hasVisibleSegments) {
        cairo_stroke(cr);
    }

    return hasVisibleSegments;
}

CairoPdfCalligraphic2DBufferRenderer::CairoPdfCalligraphic2DBufferRenderer() {
}

CairoPdfCalligraphic2DBufferRenderer::~CairoPdfCalligraphic2DBufferRenderer() {
}

double CairoPdfCalligraphic2DBufferRenderer::mmToPt(double mm) const {
    return mm * 72.0 / 25.4;
}

void CairoPdfCalligraphic2DBufferRenderer::draw(cairo_t* cr, const Calligraphic2DBuffer& icon, double xMm, double yMm, double sideMm) const {
    const StyledCalligraphic2DBuffer* styledIcon =
        dynamic_cast<const StyledCalligraphic2DBuffer*>(&icon);
    const Calligraphic2DBuffer* contourLines =
        styledIcon != nullptr ? &styledIcon->visibleContourLines() : nullptr;
    const Calligraphic2DBuffer* internalLines =
        styledIcon != nullptr ? &styledIcon->visibleInternalLines() : &icon;

    const int totalLines =
        (contourLines != nullptr ? contourLines->getNumLines() : 0) +
        (internalLines != nullptr ? internalLines->getNumLines() : 0);
    if (totalLines <= 0 || sideMm <= 0.0) {
        return;
    }

    const double originalLineWidth = cairo_get_line_width(cr);
    const double internalLineWidth = originalLineWidth * 0.5;
    const double contourLineWidth = internalLineWidth;

    double minX = 0.0;
    double maxX = 0.0;
    double minY = 0.0;
    double maxY = 0.0;
    bool initialized = false;
    if (contourLines != nullptr) {
        updateBounds(*contourLines, minX, maxX, minY, maxY, initialized);
    }
    if (internalLines != nullptr) {
        updateBounds(*internalLines, minX, maxX, minY, maxY, initialized);
    }

    if (!initialized) {
        cairo_set_line_width(cr, originalLineWidth);
        return;
    }

    const double width = maxX - minX;
    const double height = maxY - minY;
    const double reference = std::max(width, height);
    if (reference <= 0.0) {
        cairo_set_line_width(cr, originalLineWidth);
        return;
    }

    const double scale = sideMm / reference;
    const double offsetX = xMm + (sideMm - width * scale) * 0.5;
    const double offsetY = yMm + (sideMm - height * scale) * 0.5;

    cairo_tag_begin(cr, "Figure", NULL);

    if (styledIcon == nullptr && internalLines != nullptr) {
        cairo_tag_begin(cr, "Figure", NULL);
        cairo_set_source_rgb(cr, 0.0, 0.0, 0.0);
        cairo_set_line_width(cr, contourLineWidth);
        drawBuffer(cr, *internalLines, minX, minY, scale, offsetX, offsetY);
        cairo_tag_end(cr, "Figure");
    }
    else if (internalLines != nullptr || contourLines != nullptr) {
        if (internalLines != nullptr) {
            cairo_tag_begin(cr, "Figure", NULL);
            cairo_set_source_rgb(cr, 0.5, 0.5, 0.5);
            cairo_set_line_width(cr, internalLineWidth);
            drawBuffer(cr, *internalLines, minX, minY, scale, offsetX, offsetY);
            cairo_tag_end(cr, "Figure");
        }
        if (contourLines != nullptr) {
            cairo_tag_begin(cr, "Figure", NULL);
            cairo_set_source_rgb(cr, 0.0, 0.0, 0.0);
            cairo_set_line_width(cr, contourLineWidth);
            drawBuffer(cr, *contourLines, minX, minY, scale, offsetX, offsetY);
            cairo_tag_end(cr, "Figure");
        }
    }

    cairo_tag_end(cr, "Figure");
    cairo_set_line_width(cr, originalLineWidth);
}
