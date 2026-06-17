#include <cairo-pdf.h>
#include <cmath>

#include <java/util/ArrayList.txx>
#include "processing/IconGenerator.h"
#include "render/CairoPdfPageRenderer.h"
#include <vector>
#include "vsdk/toolkit/media/Calligraphic2DBuffer.h"
CairoPdfPageRenderer::CairoPdfPageRenderer()
    : labelSizeMm_(REFERENCE_LABEL_SIZE_MM),
      circleRadiusMm_(REFERENCE_LABEL_SIZE_MM * 0.1) {
    computeLayout();
}

CairoPdfPageRenderer::CairoPdfPageRenderer(double labelSizeMm, double circleRadiusMm)
    : labelSizeMm_(labelSizeMm),
      circleRadiusMm_(circleRadiusMm) {
    computeLayout();
}

CairoPdfPageRenderer::~CairoPdfPageRenderer() {
}

int
CairoPdfPageRenderer::fitCount(double pageMm, double labelSizeMm, double spacingMm) {
    if (labelSizeMm <= 0.0 || labelSizeMm > pageMm) {
        return 0;
    }
    const int n = static_cast<int>(std::floor((pageMm + spacingMm) / (labelSizeMm + spacingMm)));
    return n < 0 ? 0 : n;
}

void CairoPdfPageRenderer::computeLayout() {
    spacingMm_ = labelSizeMm_ * (REFERENCE_SPACING_MM / REFERENCE_LABEL_SIZE_MM);
    const double usableWmm = PAGE_WIDTH_MM - MARGIN_LEFT_MM - MARGIN_RIGHT_MM;
    const double usableHmm = PAGE_HEIGHT_MM - MARGIN_TOP_MM - MARGIN_BOTTOM_MM;
    columns_ = fitCount(usableWmm, labelSizeMm_, spacingMm_);
    rows_ = fitCount(usableHmm, labelSizeMm_, spacingMm_);
}

double CairoPdfPageRenderer::mmToPt(double mm) const {
    return mm * 72.0 / 25.4;
}

void CairoPdfPageRenderer::renderPage(const char* outputPdf, java::ArrayList<Label*>* labels) {
    const double cellMm = labelSizeMm_ + spacingMm_;
    const double gridWmm = columns_ > 0 ? (columns_ * labelSizeMm_ + (columns_ - 1) * spacingMm_) : 0.0;
    const double gridHmm = rows_ > 0 ? (rows_ * labelSizeMm_ + (rows_ - 1) * spacingMm_) : 0.0;
    const double usableWmm = PAGE_WIDTH_MM - MARGIN_LEFT_MM - MARGIN_RIGHT_MM;
    const double usableHmm = PAGE_HEIGHT_MM - MARGIN_TOP_MM - MARGIN_BOTTOM_MM;
    const double startXmm = MARGIN_LEFT_MM + (usableWmm - gridWmm) * 0.5;
    const double startYmm = MARGIN_TOP_MM + (usableHmm - gridHmm) * 0.5;
    const double strokeMm = REFERENCE_STROKE_MM * (labelSizeMm_ / REFERENCE_LABEL_SIZE_MM);

    cairo_surface_t* surface = cairo_pdf_surface_create(outputPdf, mmToPt(PAGE_WIDTH_MM), mmToPt(PAGE_HEIGHT_MM));
    cairo_pdf_surface_restrict_to_version(surface, CAIRO_PDF_VERSION_1_4);
    cairo_t* cr = cairo_create(surface);

    cairo_set_source_rgb(cr, 1.0, 1.0, 1.0);
    cairo_paint(cr);
    cairo_set_line_width(cr, mmToPt(strokeMm));
    long labelCount = labels ? labels->size() : 0;
    IconGenerator iconGenerator;
    std::vector<Calligraphic2DBuffer*> icons(static_cast<size_t>(labelCount), nullptr);
    double totalMaxFontSizeMm = 0.0;
    int measurableLabelCount = 0;

    int measureIdx = 0;
    for (int r = 0; r < rows_ && measureIdx < labelCount; ++r) {
        for (int c = 0; c < columns_ && measureIdx < labelCount; ++c) {
            const double rxMm = startXmm + c * cellMm;
            const double ryMm = startYmm + r * cellMm;

            Label* label = labels->get(measureIdx);
            if (label != nullptr) {
                icons[static_cast<size_t>(measureIdx)] = iconGenerator.generate(label->getTitle());
                const bool hasIcon = icons[static_cast<size_t>(measureIdx)] != nullptr;
                const double maxFontSizeMm = labelRenderer_.calculateMaxFontSizeMm(
                    cr, label->getTitle(), hasIcon, rxMm, ryMm, labelSizeMm_, circleRadiusMm_);
                if (maxFontSizeMm > 0.0) {
                    totalMaxFontSizeMm += maxFontSizeMm;
                    measurableLabelCount++;
                }
            }

            measureIdx++;
        }
    }

    const double preferredFontSizeMm =
        measurableLabelCount > 0 ? (totalMaxFontSizeMm / static_cast<double>(measurableLabelCount)) : 0.0;

    cairo_tag_begin(cr, "Figure", NULL);
    int labelIdx = 0;
    for (int r = 0; r < rows_ && labelIdx < labelCount; ++r) {
        for (int c = 0; c < columns_ && labelIdx < labelCount; ++c) {
            const double rxMm = startXmm + c * cellMm;
            const double ryMm = startYmm + r * cellMm;

            Label* label = labels->get(labelIdx);
            if (label != nullptr) {
                cairo_set_source_rgb(cr, 1.0, 0.0, 0.0);
                cairo_rectangle(cr, mmToPt(rxMm), mmToPt(ryMm), mmToPt(labelSizeMm_), mmToPt(labelSizeMm_));
                cairo_stroke(cr);
                labelRenderer_.renderRedCircle(cr, rxMm, ryMm, labelSizeMm_, circleRadiusMm_);
            }

            labelIdx++;
        }
    }
    cairo_tag_end(cr, "Figure");

    labelIdx = 0;
    for (int r = 0; r < rows_ && labelIdx < labelCount; ++r) {
        for (int c = 0; c < columns_ && labelIdx < labelCount; ++c) {
            const double rxMm = startXmm + c * cellMm;
            const double ryMm = startYmm + r * cellMm;

            Label* label = labels->get(labelIdx);
            if (label != nullptr) {
                Calligraphic2DBuffer* icon = icons[static_cast<size_t>(labelIdx)];
                labelRenderer_.renderContent(cr, *label, icon, rxMm, ryMm, labelSizeMm_, circleRadiusMm_, preferredFontSizeMm);
            }

            labelIdx++;
        }
    }

    for (size_t i = 0; i < icons.size(); ++i) {
        delete icons[i];
    }

    cairo_show_page(cr);
    cairo_destroy(cr);
    cairo_surface_destroy(surface);
}
