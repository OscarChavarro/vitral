#ifndef __CAIROPDFLABELRENDERER_H__
#define __CAIROPDFLABELRENDERER_H__

#include <string>
#include <vector>
#include <cairo.h>

#include "model/Label.h"

class Calligraphic2DBuffer;

class CairoPdfLabelRenderer {
  public:
    CairoPdfLabelRenderer();
    ~CairoPdfLabelRenderer();

    double calculateMaxFontSizeMm(cairo_t* cr, const java::String& title, bool hasIcon, double xMm, double yMm, double labelSizeMm, double circleRadiusMm) const;
    void renderContent(cairo_t* cr, const Label& label, const Calligraphic2DBuffer* icon, double xMm, double yMm, double labelSizeMm, double circleRadiusMm, double preferredFontSizeMm);
    void renderRedCircle(cairo_t* cr, double xMm, double yMm, double labelSizeMm, double circleRadiusMm) const;

  private:
    struct TextLayout {
        double centerXmm;
        double centerYmm;
        double fontSizeMm;
        double lineHeightMm;
        double firstBaselineMm;
    };

    double mmToPt(double mm) const;
    TextLayout calculateTextLayout(
        cairo_t* cr,
        const java::String& title,
        bool hasIcon,
        double xMm,
        double yMm,
        double labelSizeMm,
        double circleRadiusMm,
        double preferredFontSizeMm) const;
    void renderTitle(
        cairo_t* cr,
        const java::String& title,
        const TextLayout& layout) const;
    std::vector<std::string> splitLines(const char* text) const;
};

#endif
