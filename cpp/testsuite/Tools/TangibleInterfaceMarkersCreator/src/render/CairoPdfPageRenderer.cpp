#include <cairo-pdf.h>
#include <cmath>

#include "java/util/ArrayList.txx"
#include "render/CairoPdfPageRenderer.h"
CairoPdfPageRenderer::CairoPdfPageRenderer()
    : markerSizeMm(REFERENCE_MARKER_SIZE_MM) {
    computeLayout();
}

CairoPdfPageRenderer::CairoPdfPageRenderer(double markerSizeMm)
    : markerSizeMm(markerSizeMm) {
    computeLayout();
}

CairoPdfPageRenderer::~CairoPdfPageRenderer() {
}

int
CairoPdfPageRenderer::fitCount(double pageMm, double markerSizeMm, double spacingMm) {
    if (markerSizeMm <= 0.0 || markerSizeMm > pageMm) {
        return 0;
    }
    const int n = static_cast<int>(std::floor((pageMm + spacingMm) / (markerSizeMm + spacingMm)));
    return n < 0 ? 0 : n;
}

void CairoPdfPageRenderer::computeLayout() {
    spacingMm = markerSizeMm * (REFERENCE_SPACING_MM / REFERENCE_MARKER_SIZE_MM);
    const double usableWmm = PAGE_WIDTH_MM - MARGIN_LEFT_MM - MARGIN_RIGHT_MM;
    const double usableHmm = PAGE_HEIGHT_MM - MARGIN_TOP_MM - MARGIN_BOTTOM_MM;
    columns = fitCount(usableWmm, markerSizeMm, spacingMm);
    rows = fitCount(usableHmm, markerSizeMm, spacingMm);
}

double CairoPdfPageRenderer::mmToPt(double mm) const {
    return mm * 72.0 / 25.4;
}

void CairoPdfPageRenderer::renderPage(const char* outputPdf, java::ArrayList<Marker*>* markers) {
    const double cellMm = markerSizeMm + spacingMm;
    const double gridWmm = columns > 0 ? (columns * markerSizeMm + (columns - 1) * spacingMm) : 0.0;
    const double gridHmm = rows > 0 ? (rows * markerSizeMm + (rows - 1) * spacingMm) : 0.0;
    const double usableWmm = PAGE_WIDTH_MM - MARGIN_LEFT_MM - MARGIN_RIGHT_MM;
    const double usableHmm = PAGE_HEIGHT_MM - MARGIN_TOP_MM - MARGIN_BOTTOM_MM;
    const double startXmm = MARGIN_LEFT_MM + (usableWmm - gridWmm) * 0.5;
    const double startYmm = MARGIN_TOP_MM + (usableHmm - gridHmm) * 0.5;

    cairo_surface_t* surface = cairo_pdf_surface_create(outputPdf, mmToPt(PAGE_WIDTH_MM), mmToPt(PAGE_HEIGHT_MM));
    cairo_pdf_surface_restrict_to_version(surface, CAIRO_PDF_VERSION_1_4);
    cairo_t* cr = cairo_create(surface);

    cairo_set_source_rgb(cr, 1.0, 1.0, 1.0);
    cairo_paint(cr);

    const double strokeMm = REFERENCE_STROKE_MM * (markerSizeMm / REFERENCE_MARKER_SIZE_MM);
    cairo_set_line_width(cr, mmToPt(strokeMm));

    int markerIdx = 0;
    long markerCount = markers ? markers->size() : 0;
    for (int r = 0; r < rows && markerIdx < markerCount; ++r) {
        for (int c = 0; c < columns && markerIdx < markerCount; ++c) {
            const double rxMm = startXmm + c * cellMm;
            const double ryMm = startYmm + r * cellMm;

            cairo_set_source_rgb(cr, 1.0, 0.0, 0.0);
            cairo_rectangle(cr, mmToPt(rxMm), mmToPt(ryMm), mmToPt(markerSizeMm), mmToPt(markerSizeMm));
            cairo_stroke(cr);

            Marker* marker = markers->get(markerIdx);
            markerRenderer.render(cr, *marker, rxMm, ryMm, markerSizeMm);

            markerIdx++;
        }
    }

    cairo_show_page(cr);
    cairo_destroy(cr);
    cairo_surface_destroy(surface);
}
