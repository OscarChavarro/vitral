#ifndef __CAIRO_PDF_CALLIGRAPHIC_2_D_BUFFER_RENDERER__
#define __CAIRO_PDF_CALLIGRAPHIC_2_D_BUFFER_RENDERER__

#include <cairo.h>
class Calligraphic2DBuffer;

class CairoPdfCalligraphic2DBufferRenderer {
  public:
    CairoPdfCalligraphic2DBufferRenderer();
    ~CairoPdfCalligraphic2DBufferRenderer();

    void draw(cairo_t* cr, const Calligraphic2DBuffer& icon, double xMm, double yMm, double sideMm) const;

  private:
    void updateBounds(
        const Calligraphic2DBuffer& icon,
        double& minX,
        double& maxX,
        double& minY,
        double& maxY,
        bool& initialized) const;
    bool drawBuffer(
        cairo_t* cr,
        const Calligraphic2DBuffer& icon,
        double minX,
        double minY,
        double scale,
        double offsetX,
        double offsetY) const;
    double mmToPt(double mm) const;
};

#endif
