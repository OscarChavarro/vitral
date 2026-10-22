#include <math.h>

#include "vsdk/toolkit/gui/viewport/ViewportElementScaler.h"
#include "vsdk/toolkit/common/Cpp98Support.h"

ViewportElementScaler::ViewportElementScaler()
    : screenWidthInPixels(0), screenHeightInPixels(0),
      referenceWidthInPixels(1024), referenceHeightInPixels(768),
      scaleStep(0.25), maximumScale(4.0)
{
}

int ViewportElementScaler::getScreenWidthInPixels() const
{
    return screenWidthInPixels;
}

int ViewportElementScaler::getScreenHeightInPixels() const
{
    return screenHeightInPixels;
}

void ViewportElementScaler::setScreenResolution(int screenWidthInPixels,
                                                int screenHeightInPixels)
{
    this->screenWidthInPixels = screenWidthInPixels;
    this->screenHeightInPixels = screenHeightInPixels;
}

int ViewportElementScaler::getReferenceWidthInPixels() const
{
    return referenceWidthInPixels;
}

int ViewportElementScaler::getReferenceHeightInPixels() const
{
    return referenceHeightInPixels;
}

void ViewportElementScaler::setReferenceResolution(
    int referenceWidthInPixels, int referenceHeightInPixels)
{
    if ( referenceWidthInPixels > 0 && referenceHeightInPixels > 0 ) {
        this->referenceWidthInPixels = referenceWidthInPixels;
        this->referenceHeightInPixels = referenceHeightInPixels;
    }
}

double ViewportElementScaler::getScaleStep() const
{
    return scaleStep;
}

void ViewportElementScaler::setScaleStep(double scaleStep)
{
    if ( scaleStep > 0 ) {
        this->scaleStep = scaleStep;
    }
}

double ViewportElementScaler::getMaximumScale() const
{
    return maximumScale;
}

void ViewportElementScaler::setMaximumScale(double maximumScale)
{
    if ( maximumScale >= 1.0 ) {
        this->maximumScale = maximumScale;
    }
}

double ViewportElementScaler::getScale() const
{
    if ( screenWidthInPixels <= 0 || screenHeightInPixels <= 0 ) {
        return 1.0;
    }

    double widthRatio = ((double)screenWidthInPixels) /
        ((double)referenceWidthInPixels);
    double heightRatio = ((double)screenHeightInPixels) /
        ((double)referenceHeightInPixels);
    double scale = Cpp98Support::fmin(widthRatio, heightRatio);

    // floor(v + 0.5) mimics Java's Math.round
    scale = floor(scale / scaleStep + 0.5) * scaleStep;
    return Cpp98Support::fmax(1.0, Cpp98Support::fmin(maximumScale, scale));
}

double ViewportElementScaler::scaleLength(double baseLength) const
{
    return baseLength * getScale();
}

int ViewportElementScaler::scaleSize(int baseSize) const
{
    int size = (int)floor(((double)baseSize) * getScale() + 0.5);
    return size > 1 ? size : 1;
}
