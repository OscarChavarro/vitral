package vsdk.toolkit.gui.viewport;

/**
A `ViewportElementScaler` calculates how much the elements of a viewport (text and its related
lengths) must be enlarged for the resolution of a screen.

Sizes in the application are designed for a legacy reference resolution
(1024x768 by default; 640x480 and 800x600 screens are also served with the
designed sizes). On screens with more pixels the same size in pixels looks
tiny, so the scale grows with the resolution: it is the smaller of
the width and height ratios between the screen and the reference resolution,
rounded to steps of a quarter, and never below 1 (sizes are not reduced for
legacy resolutions). For example, a 2560x1600 screen gets a scale of 2, and
a 1920x1080 one gets 1.5.

The class is a plain model object: it does not know how the resolution is
obtained, since that depends on the platform (for example, an AWT based
provider can be built on top of `java.awt.Toolkit`).
Whoever knows it informs the scaler with `setScreenResolution`, and can do it
again if the resolution changes, so the scale follows the screen. Until a
valid resolution is given, the scale is 1.
*/
public class ViewportElementScaler
{
    private int screenWidthInPixels;
    private int screenHeightInPixels;
    private int referenceWidthInPixels;
    private int referenceHeightInPixels;
    private double scaleStep;
    private double maximumScale;

    public ViewportElementScaler()
    {
        screenWidthInPixels = 0;
        screenHeightInPixels = 0;
        referenceWidthInPixels = 1024;
        referenceHeightInPixels = 768;
        scaleStep = 0.25;
        maximumScale = 4.0;
    }

    public int getScreenWidthInPixels()
    {
        return screenWidthInPixels;
    }

    public int getScreenHeightInPixels()
    {
        return screenHeightInPixels;
    }

    /**
    Informs the resolution, in physical pixels, of the screen where text is
    presented.
    */
    public void setScreenResolution(int screenWidthInPixels,
                                    int screenHeightInPixels)
    {
        this.screenWidthInPixels = screenWidthInPixels;
        this.screenHeightInPixels = screenHeightInPixels;
    }

    public int getReferenceWidthInPixels()
    {
        return referenceWidthInPixels;
    }

    public int getReferenceHeightInPixels()
    {
        return referenceHeightInPixels;
    }

    /**
    Sets the resolution for which the base sizes were designed; invalid
    (not positive) values are ignored.
    */
    public void setReferenceResolution(int referenceWidthInPixels,
                                       int referenceHeightInPixels)
    {
        if ( referenceWidthInPixels > 0 && referenceHeightInPixels > 0 ) {
            this.referenceWidthInPixels = referenceWidthInPixels;
            this.referenceHeightInPixels = referenceHeightInPixels;
        }
    }

    public double getScaleStep()
    {
        return scaleStep;
    }

    /**
    @param scaleStep the scale is rounded to a multiple of this value; not
    positive values are ignored
    */
    public void setScaleStep(double scaleStep)
    {
        if ( scaleStep > 0 ) {
            this.scaleStep = scaleStep;
        }
    }

    public double getMaximumScale()
    {
        return maximumScale;
    }

    /**
    @param maximumScale upper limit of the scale; values under 1 are ignored
    */
    public void setMaximumScale(double maximumScale)
    {
        if ( maximumScale >= 1.0 ) {
            this.maximumScale = maximumScale;
        }
    }

    /**
    @return the factor to apply to base sizes for the current screen
    resolution, between 1 and the maximum scale
    */
    public double getScale()
    {
        if ( screenWidthInPixels <= 0 || screenHeightInPixels <= 0 ) {
            return 1.0;
        }

        double widthRatio = ((double)screenWidthInPixels) /
            ((double)referenceWidthInPixels);
        double heightRatio = ((double)screenHeightInPixels) /
            ((double)referenceHeightInPixels);
        double scale = Math.min(widthRatio, heightRatio);

        scale = Math.round(scale / scaleStep) * scaleStep;
        return Math.max(1.0, Math.min(maximumScale, scale));
    }

    /**
    @param baseLength a length (line width, etc.) in pixels, designed for the
    reference resolution, that can have fractional values
    @return the length to use in the current screen
    */
    public double scaleLength(double baseLength)
    {
        return baseLength * getScale();
    }

    /**
    @param baseSize a size (font size, offset, etc.) in pixels, designed for
    the reference resolution
    @return the size to use in the current screen, at least 1
    */
    public int scaleSize(int baseSize)
    {
        return Math.max(1, (int)Math.round((baseSize) * getScale()));
    }
}
