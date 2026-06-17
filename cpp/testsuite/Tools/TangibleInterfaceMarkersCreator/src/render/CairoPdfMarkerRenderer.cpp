#include "java/util/ArrayList.txx"
#include "render/CairoPdfMarkerRenderer.h"
#include "vsdk/toolkit/environment/geometry/surface/polygon/_Polygon2DContour.h"
CairoPdfMarkerRenderer::CairoPdfMarkerRenderer() {
}

double CairoPdfMarkerRenderer::mmToPt(double mm) const {
    return mm * 72.0 / 25.4;
}

void CairoPdfMarkerRenderer::drawFilledRectMm(cairo_t* cr, double xMm, double yMm, double wMm, double hMm) const {
    cairo_rectangle(cr, mmToPt(xMm), mmToPt(yMm), mmToPt(wMm), mmToPt(hMm));
    cairo_fill(cr);
}

Polygon2D* CairoPdfMarkerRenderer::buildRect(double xMm, double yMm, double wMm, double hMm) const
{
    Polygon2D* poly = new Polygon2D();
    poly->addVertex(xMm, yMm);
    poly->addVertex(xMm + wMm, yMm);
    poly->addVertex(xMm + wMm, yMm + hMm);
    poly->addVertex(xMm, yMm + hMm);
    return poly;
}

void CairoPdfMarkerRenderer::appendPolygonPathMm(cairo_t* cr, const Polygon2D& polygon) const
{
    for (long i = 0; i < polygon.loops.size(); ++i) {
        _Polygon2DContour* loop = polygon.loops.get(i);
        if (!loop || loop->vertices.size() <= 0) continue;
        const Vertex2D first = loop->vertices.get(0);
        cairo_move_to(cr, mmToPt(first.x), mmToPt(first.y));
        for (long j = 1; j < loop->vertices.size(); ++j) {
            const Vertex2D p = loop->vertices.get(j);
            cairo_line_to(cr, mmToPt(p.x), mmToPt(p.y));
        }
        cairo_close_path(cr);
    }
}

void CairoPdfMarkerRenderer::render(cairo_t* cr, const Marker& marker, double xMm, double yMm, double markerSizeMm) {
    const double tagSizeMm = markerSizeMm * TAG_TO_MARKER_RATIO;
    const double cellMm = tagSizeMm / GRID_SIZE;
    const double offsetXTag = (markerSizeMm - tagSizeMm) / 2.0;
    const double offsetYTag = (markerSizeMm - tagSizeMm) / 2.0;

    // White background
    cairo_set_source_rgb(cr, 1.0, 1.0, 1.0);
    drawFilledRectMm(cr, xMm, yMm, markerSizeMm, markerSizeMm);

    java::ArrayList<Polygon2D*> markerParts;
    double borderX = xMm + offsetXTag;
    double borderY = yMm + offsetYTag;
    for (int y = 0; y < GRID_SIZE; ++y) {
        for (int x = 0; x < GRID_SIZE; ++x) {
            if (x != 0 && x != GRID_SIZE - 1 && y != 0 && y != GRID_SIZE - 1) continue;
            markerParts.add(buildRect(borderX + x * cellMm, borderY + y * cellMm, cellMm, cellMm));
        }
    }

    Grid* grid = marker.getGrid();
    if (grid) {
        for (int y = 0; y < Grid::SIZE; ++y) {
            for (int x = 0; x < Grid::SIZE; ++x) {
                if (!grid->get(x, y)) continue;

                double px = borderX + cellMm + x * cellMm;
                double py = borderY + cellMm + y * cellMm;
                markerParts.add(buildRect(px, py, cellMm, cellMm));
            }
        }
    }

    Polygon2D merged = polygonProcessor_.execute(&markerParts);
    cairo_set_source_rgb(cr, 0.0, 0.0, 0.0);
    cairo_fill_rule_t oldRule = cairo_get_fill_rule(cr);
    cairo_set_fill_rule(cr, CAIRO_FILL_RULE_EVEN_ODD);
    appendPolygonPathMm(cr, merged);
    cairo_fill(cr);
    cairo_set_fill_rule(cr, oldRule);

    for (long i = 0; i < markerParts.size(); ++i) {
        delete markerParts.get(i);
    }
}
