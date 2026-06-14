#include "render/CairoPdfLabelRenderer.h"

#include <cmath>
#include <algorithm>
#include <string>
#include <vector>

#include "render/CairoPdfCalligraphic2DBufferRenderer.h"
#include "vsdk/toolkit/media/Calligraphic2DBuffer.h"

const double kIconAreaRatio = 0.34;

std::vector<std::string> CairoPdfLabelRenderer::splitLines(const char* text) const {
    std::vector<std::string> lines;
    std::string current;

    for (const char* p = text; *p != '\0'; ++p) {
        if (*p == '\n') {
            lines.push_back(current);
            current.clear();
        } else {
            current.push_back(*p);
        }
    }
    lines.push_back(current);

    return lines;
}

CairoPdfLabelRenderer::CairoPdfLabelRenderer() {
}

CairoPdfLabelRenderer::~CairoPdfLabelRenderer() {
}

double CairoPdfLabelRenderer::mmToPt(double mm) const {
    return mm * 72.0 / 25.4;
}

double CairoPdfLabelRenderer::calculateMaxFontSizeMm(
    cairo_t* cr,
    const java::String& title,
    bool hasIcon,
    double xMm,
    double yMm,
    double labelSizeMm,
    double circleRadiusMm) const {
    const std::vector<std::string> lines = splitLines(title.toCString());
    const double marginMm = labelSizeMm * 0.08;
    const double contentTopMm = yMm + marginMm;
    const double circleTopMm = yMm + labelSizeMm * 0.5 - circleRadiusMm;
    const double iconSideMm = hasIcon ? labelSizeMm * kIconAreaRatio : 0.0;
    const double textGapMm = hasIcon ? marginMm * 0.5 : 0.0;
    const double textLeftMm = hasIcon ? (xMm + marginMm + iconSideMm + textGapMm) : (xMm + marginMm);
    const double textRightMm = xMm + labelSizeMm - marginMm;
    const double textTopMm = contentTopMm;
    const double textBottomMm = circleTopMm - marginMm;
    const double textWidthMm = std::max(0.0, textRightMm - textLeftMm);
    const double textHeightMm = std::max(0.0, textBottomMm - textTopMm);
    const double lineHeightFactor = 1.15;

    cairo_select_font_face(cr, "Helvetica", CAIRO_FONT_SLANT_NORMAL, CAIRO_FONT_WEIGHT_BOLD);
    cairo_set_font_size(cr, mmToPt(1.0));

    double maxLineWidthMmAtOneMm = 0.0;
    for (size_t i = 0; i < lines.size(); ++i) {
        cairo_text_extents_t extents;
        cairo_text_extents(cr, lines[i].c_str(), &extents);
        const double lineWidthMm = extents.width * 25.4 / 72.0;
        maxLineWidthMmAtOneMm = std::max(maxLineWidthMmAtOneMm, lineWidthMm);
    }

    const double blockHeightAtOneMm =
        1.0 + (static_cast<double>(lines.size()) - 1.0) * lineHeightFactor;
    const double widthLimitedFontSizeMm =
        maxLineWidthMmAtOneMm > 0.0 ? (textWidthMm / maxLineWidthMmAtOneMm) : textWidthMm;
    const double heightLimitedFontSizeMm =
        blockHeightAtOneMm > 0.0 ? (textHeightMm / blockHeightAtOneMm) : textHeightMm;

    if (widthLimitedFontSizeMm <= 0.0 || heightLimitedFontSizeMm <= 0.0) {
        return 0.0;
    }
    return std::min(widthLimitedFontSizeMm, heightLimitedFontSizeMm);
}

void CairoPdfLabelRenderer::renderTitle(cairo_t* cr, const java::String& title, const TextLayout& layout) const {
    const std::vector<std::string> lines = splitLines(title.toCString());
    cairo_select_font_face(cr, "Helvetica", CAIRO_FONT_SLANT_NORMAL, CAIRO_FONT_WEIGHT_BOLD);
    cairo_set_font_size(cr, mmToPt(layout.fontSizeMm));
    cairo_set_source_rgb(cr, 0.0, 0.0, 0.0);

    for (size_t i = 0; i < lines.size(); ++i) {
        cairo_text_extents_t extents;
        cairo_text_extents(cr, lines[i].c_str(), &extents);
        const double centeredXmm = layout.centerXmm - (extents.width / 2.0 + extents.x_bearing) * 25.4 / 72.0;
        const double baselineYmm = layout.firstBaselineMm + layout.lineHeightMm * static_cast<double>(i);
        cairo_move_to(cr, mmToPt(centeredXmm), mmToPt(baselineYmm));
        cairo_show_text(cr, lines[i].c_str());
    }
}

CairoPdfLabelRenderer::TextLayout CairoPdfLabelRenderer::calculateTextLayout(
    cairo_t* cr,
    const java::String& title,
    bool hasIcon,
    double xMm,
    double yMm,
    double labelSizeMm,
    double circleRadiusMm,
    double preferredFontSizeMm) const {
    const std::vector<std::string> lines = splitLines(title.toCString());
    const double marginMm = labelSizeMm * 0.08;
    const double contentTopMm = yMm + marginMm;
    const double circleTopMm = yMm + labelSizeMm * 0.5 - circleRadiusMm;
    const double iconSideMm = hasIcon ? labelSizeMm * kIconAreaRatio : 0.0;
    const double textGapMm = hasIcon ? marginMm * 0.5 : 0.0;
    const double textLeftMm = hasIcon ? (xMm + marginMm + iconSideMm + textGapMm) : (xMm + marginMm);
    const double textRightMm = xMm + labelSizeMm - marginMm;
    const double textTopMm = contentTopMm;
    const double textBottomMm = circleTopMm - marginMm;
    const double textWidthMm = std::max(0.0, textRightMm - textLeftMm);
    const double textHeightMm = std::max(0.0, textBottomMm - textTopMm);
    double fontSizeMm = calculateMaxFontSizeMm(cr, title, hasIcon, xMm, yMm, labelSizeMm, circleRadiusMm);
    const double lineHeightFactor = 1.15;
    if (preferredFontSizeMm > 0.0) {
        fontSizeMm = std::min(fontSizeMm, preferredFontSizeMm);
    }

    cairo_set_font_size(cr, mmToPt(fontSizeMm));

    const double lineHeightMm = fontSizeMm * lineHeightFactor;
    const double textBlockHeightMm = fontSizeMm + (static_cast<double>(lines.size()) - 1.0) * lineHeightMm;
    const double textAreaCenterMm = textTopMm + textHeightMm * 0.5;
    double firstBaselineMm = textAreaCenterMm - textBlockHeightMm * 0.5 + fontSizeMm * 0.5;
    firstBaselineMm = std::max(firstBaselineMm, textTopMm + fontSizeMm);

    TextLayout layout;
    layout.centerXmm = hasIcon ? (textLeftMm + textWidthMm * 0.5) : (xMm + labelSizeMm * 0.5);
    layout.centerYmm = textAreaCenterMm;
    layout.fontSizeMm = fontSizeMm;
    layout.lineHeightMm = lineHeightMm;
    layout.firstBaselineMm = firstBaselineMm;
    return layout;
}

void CairoPdfLabelRenderer::renderRedCircle(cairo_t* cr, double xMm, double yMm, double labelSizeMm, double circleRadiusMm) const {
    const double centerXmm = xMm + labelSizeMm * 0.5;
    const double centerYmm = yMm + labelSizeMm * 0.5;

    cairo_set_source_rgb(cr, 1.0, 0.0, 0.0);
    cairo_arc(cr, mmToPt(centerXmm), mmToPt(centerYmm), mmToPt(circleRadiusMm), 0.0, 2.0 * M_PI);
    cairo_stroke(cr);
}

void CairoPdfLabelRenderer::renderContent(cairo_t* cr, const Label& label, const Calligraphic2DBuffer* icon, double xMm, double yMm, double labelSizeMm, double circleRadiusMm, double preferredFontSizeMm) {
    const bool hasIcon = icon != nullptr;
    const TextLayout layout = calculateTextLayout(cr, label.getTitle(), hasIcon, xMm, yMm, labelSizeMm, circleRadiusMm, preferredFontSizeMm);

    if (hasIcon) {
        CairoPdfCalligraphic2DBufferRenderer iconRenderer;
        const double iconSideMm = labelSizeMm * kIconAreaRatio;
        const double marginMm = labelSizeMm * 0.08;
        const double iconXmm = xMm + marginMm;
        const double iconYmm = layout.centerYmm - iconSideMm * 0.5;
        cairo_set_source_rgb(cr, 0.0, 0.0, 0.0);
        iconRenderer.draw(cr, *icon, iconXmm, iconYmm, iconSideMm);
    }

    renderTitle(cr, label.getTitle(), layout);
}
