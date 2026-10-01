import { Math as JavaMath } from "../../../../java/lang/Math.js";

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
export class ViewportElementScaler {
    private screenWidthInPixels: number;
    private screenHeightInPixels: number;
    private referenceWidthInPixels: number;
    private referenceHeightInPixels: number;
    private scaleStep: number;
    private maximumScale: number;

    public constructor() {
        this.screenWidthInPixels = 0;
        this.screenHeightInPixels = 0;
        this.referenceWidthInPixels = 1024;
        this.referenceHeightInPixels = 768;
        this.scaleStep = 0.25;
        this.maximumScale = 4.0;
    }

    public getScreenWidthInPixels(): number {
        return this.screenWidthInPixels;
    }

    public getScreenHeightInPixels(): number {
        return this.screenHeightInPixels;
    }

    /**
    Informs the resolution, in physical pixels, of the screen where text is
    presented.
    */
    public setScreenResolution(screenWidthInPixels: number, screenHeightInPixels: number): void {
        this.screenWidthInPixels = screenWidthInPixels;
        this.screenHeightInPixels = screenHeightInPixels;
    }

    public getReferenceWidthInPixels(): number {
        return this.referenceWidthInPixels;
    }

    public getReferenceHeightInPixels(): number {
        return this.referenceHeightInPixels;
    }

    /**
    Sets the resolution for which the base sizes were designed; invalid
    (not positive) values are ignored.
    */
    public setReferenceResolution(referenceWidthInPixels: number, referenceHeightInPixels: number): void {
        if (referenceWidthInPixels > 0 && referenceHeightInPixels > 0) {
            this.referenceWidthInPixels = referenceWidthInPixels;
            this.referenceHeightInPixels = referenceHeightInPixels;
        }
    }

    public getScaleStep(): number {
        return this.scaleStep;
    }

    /**
    @param scaleStep the scale is rounded to a multiple of this value; not
    positive values are ignored
    */
    public setScaleStep(scaleStep: number): void {
        if (scaleStep > 0) {
            this.scaleStep = scaleStep;
        }
    }

    public getMaximumScale(): number {
        return this.maximumScale;
    }

    /**
    @param maximumScale upper limit of the scale; values under 1 are ignored
    */
    public setMaximumScale(maximumScale: number): void {
        if (maximumScale >= 1.0) {
            this.maximumScale = maximumScale;
        }
    }

    /**
    @return the factor to apply to base sizes for the current screen
    resolution, between 1 and the maximum scale
    */
    public getScale(): number {
        if (this.screenWidthInPixels <= 0 || this.screenHeightInPixels <= 0) {
            return 1.0;
        }

        const widthRatio: number = this.screenWidthInPixels / this.referenceWidthInPixels;
        const heightRatio: number = this.screenHeightInPixels / this.referenceHeightInPixels;
        let scale: number = Math.min(widthRatio, heightRatio);

        scale = JavaMath.round(scale / this.scaleStep) * this.scaleStep;
        return Math.max(1.0, Math.min(this.maximumScale, scale));
    }

    /**
    @param baseLength a length (line width, etc.) in pixels, designed for the
    reference resolution, that can have fractional values
    @return the length to use in the current screen
    */
    public scaleLength(baseLength: number): number {
        return baseLength * this.getScale();
    }

    /**
    @param baseSize a size (font size, offset, etc.) in pixels, designed for
    the reference resolution
    @return the size to use in the current screen, at least 1
    */
    public scaleSize(baseSize: number): number {
        return Math.max(1, Math.trunc(JavaMath.round(baseSize * this.getScale())));
    }
}
