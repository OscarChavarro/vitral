#ifndef __CAIRO_PDF_MARKER_RENDERER__
#define __CAIRO_PDF_MARKER_RENDERER__

#include <cairo.h>

#include "model/Marker.h"
#include "processing/ConstructivePolygonGeometryProcessor.h"
class CairoPdfMarkerRenderer {
public:
    CairoPdfMarkerRenderer();

    void render(cairo_t* cr, const Marker& marker, double xMm, double yMm, double markerSizeMm);

private:
    // The printed AprilTag occupies 35mm inside the 40mm red frame in the
    // reference design; it scales proportionally with the marker size.
    static constexpr double TAG_TO_MARKER_RATIO = 35.0 / 40.0;
    static constexpr double BORDER_THICKNESS_FACTOR = 1.0 / 8.0;
    static constexpr int GRID_SIZE = 8;
    ConstructivePolygonGeometryProcessor polygonProcessor_;

    double mmToPt(double mm) const;
    void drawFilledRectMm(cairo_t* cr, double xMm, double yMm, double wMm, double hMm) const;
    Polygon2D* buildRect(double xMm, double yMm, double wMm, double hMm) const;
    void appendPolygonPathMm(cairo_t* cr, const Polygon2D& polygon) const;
};

#endif
