#ifndef __CAIROPDFPAGERENDERER_H__
#define __CAIROPDFPAGERENDERER_H__

#include <java/util/ArrayList.h>
#include "model/Marker.h"
#include "render/CairoPdfMarkerRenderer.h"
class CairoPdfPageRenderer {
public:
    CairoPdfPageRenderer();
    explicit CairoPdfPageRenderer(double markerSizeMm);
    ~CairoPdfPageRenderer();

    // Number of marker columns/rows that fit on the A4 page for the configured
    // marker size. A zero in either dimension means not even one marker fits.
    int getColumns() const { return columns_; }
    int getRows() const { return rows_; }
    int getCapacity() const { return columns_ * rows_; }

    double getMarkerSizeMm() const { return markerSizeMm_; }

    void renderPage(const char* outputPdf, java::ArrayList<Marker*>* markers);

private:
    static constexpr double PAGE_WIDTH_MM = 210.0;
    static constexpr double PAGE_HEIGHT_MM = 297.0;
    // Page margins: the markers are laid out inside this printable area, never
    // edge to edge. Top is larger than the other sides.
    static constexpr double MARGIN_TOP_MM = 40.0;
    static constexpr double MARGIN_BOTTOM_MM = 30.0;
    static constexpr double MARGIN_LEFT_MM = 30.0;
    static constexpr double MARGIN_RIGHT_MM = 30.0;
    // Reference design. Spacing and stroke width scale proportionally with the
    // marker size so the layout looks the same at any scale.
    static constexpr double REFERENCE_MARKER_SIZE_MM = 40.0;
    static constexpr double REFERENCE_SPACING_MM = 5.0;
    static constexpr double REFERENCE_STROKE_MM = 0.6;

    double markerSizeMm_;
    double spacingMm_;
    int columns_;
    int rows_;

    CairoPdfMarkerRenderer markerRenderer_;

    void computeLayout();
    double mmToPt(double mm) const;
    // Maximum number of markers of markerSizeMm (with spacingMm between them)
    // that fit along an axis of length pageMm. Returns 0 if none fit.
    static int fitCount(double pageMm, double markerSizeMm, double spacingMm);
};

#endif
