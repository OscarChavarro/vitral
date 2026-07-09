#ifndef __CAIRO_PDF_PAGE_RENDERER__
#define __CAIRO_PDF_PAGE_RENDERER__

#include <java/util/ArrayList.h>
#include "model/Label.h"
#include "render/CairoPdfLabelRenderer.h"
class CairoPdfPageRenderer {
  public:
    CairoPdfPageRenderer();
    explicit CairoPdfPageRenderer(double labelSizeMm, double circleRadiusMm);
    ~CairoPdfPageRenderer();

    int getColumns() const { return columns_; }
    int getRows() const { return rows_; }
    int getCapacity() const { return columns_ * rows_; }

    double getLabelSizeMm() const { return labelSizeMm_; }

    void renderPage(const char* outputPdf, java::ArrayList<Label*>* labels);

  private:
    static constexpr double PAGE_WIDTH_MM = 210.0;
    static constexpr double PAGE_HEIGHT_MM = 297.0;
    static constexpr double MARGIN_TOP_MM = 40.0;
    static constexpr double MARGIN_BOTTOM_MM = 30.0;
    static constexpr double MARGIN_LEFT_MM = 30.0;
    static constexpr double MARGIN_RIGHT_MM = 30.0;
    static constexpr double REFERENCE_LABEL_SIZE_MM = 40.0;
    static constexpr double REFERENCE_SPACING_MM = 5.0;
    static constexpr double REFERENCE_STROKE_MM = 0.6;

    double labelSizeMm_;
    double circleRadiusMm_;
    double spacingMm_;
    int columns_;
    int rows_;

    CairoPdfLabelRenderer labelRenderer_;

    void computeLayout();
    double mmToPt(double mm) const;
    static int fitCount(double pageMm, double labelSizeMm, double spacingMm);
};

#endif
