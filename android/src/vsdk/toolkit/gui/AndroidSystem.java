package vsdk.toolkit.gui;

// Android packages
import android.view.MotionEvent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
//import android.graphics.Paint.FontMetrics;
import android.graphics.Typeface;

// VSDK Classes
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4;
import vsdk.toolkit.media.RGBAImageUncompressed;
import vsdk.toolkit.media.RGBAPixel;
import vsdk.toolkit.render.android.AndroidRGBAImageUncompressedRenderer;

public class AndroidSystem extends PresentationElement
{
    public static MouseEvent android2vsdkEvent(MotionEvent eandroid)
    {
        MouseEvent evsdk;

        evsdk = new MouseEvent();
        evsdk.setX((int)Math.floor(eandroid.getX()));
        evsdk.setY((int)Math.floor(eandroid.getY()));
        evsdk.setButton(MouseEvent.BUTTON1);
        evsdk.setModifiers(MouseEvent.BUTTON1_DOWN_MASK);

        return evsdk;
    }

    public static RGBAImageUncompressed calculateLabelImage(String label,
        ColorRgb foregroundColor, ColorRgb backgroundColor)
    {
        return calculateLabelImage(label, foregroundColor, backgroundColor, 14);
    }

    /**
    Given a new desired unicode (UTF-16) character and a raster bitmapped font
    map image, this methods adds a new corresponding glyph to font map.

    This method also computes a texture transform to be applied to font map when
    drawing the glyph on a unit square.

    On null or full glyph map, this method creates a new map.
    @param inOutFontMap
    @param inOutCurrentPosition a two sized array. currentPos[0] is next x span
    for glyph, currentPos[1] is y span for glyph.
    @param inOutGlyphTextureTransform
    @param inCharacter
    @param inForegroundColor
    @param inBackgroundColor
    @param inFontSize
    @return
    */
    public static RGBAImageUncompressed addGlyphToFontMap(
        RGBAImageUncompressed inOutFontMap,
        int inOutCurrentPosition[],
        Matrix4x4[] inOutGlyphTextureTransform,
        final String inCharacter,
        final ColorRgb inForegroundColor,
        final ColorRgb inBackgroundColor,
        final int inFontSize)
    {
        // Prepare raster font map and coordinates for drawing
        if ( inOutFontMap == null ) {
            inOutFontMap = new RGBAImageUncompressed();
            inOutFontMap.init(256, 256);
            fillTransparent(inOutFontMap);
            inOutCurrentPosition[0] = inOutCurrentPosition[1] = 0;
        }
        RGBAImageUncompressed newGlyph = calculateLabelImage(
            inCharacter, inForegroundColor, inBackgroundColor, inFontSize);
        if ( inOutCurrentPosition[1] + 2*newGlyph.getYSize() >
             inOutFontMap.getYSize() ) {
            inOutFontMap = new RGBAImageUncompressed();
            inOutFontMap.init(256, 256);
            fillTransparent(inOutFontMap);
            inOutCurrentPosition[0] = inOutCurrentPosition[1] = 0;
        }
        if ( inOutCurrentPosition[0] + newGlyph.getXSize() >
             inOutFontMap.getYSize() ) {
            inOutCurrentPosition[0] = 1;
            inOutCurrentPosition[1] += newGlyph.getYSize() + 1;
        }

        // Transfer current glyph to current raster font map
        int x;
        int y;
        RGBAPixel p = new RGBAPixel();
        for ( y = 0; y < newGlyph.getYSize(); y++ ) {
            for ( x = 0; x < newGlyph.getXSize(); x++ ) {
                newGlyph.getPixelRgba(x, y, p);
                inOutFontMap.putPixel(
                    x + inOutCurrentPosition[0],
                    y + inOutCurrentPosition[1], p);
            }
        }

        // Compute transform
        Matrix4x4 S = new Matrix4x4();
        Matrix4x4 T = new Matrix4x4();
        double ux = inOutFontMap.getXSize();
        double uy = inOutFontMap.getYSize();
        double dx = newGlyph.getXSize()+1;
        double dy = newGlyph.getYSize();
        double px = inOutCurrentPosition[0];
        double py = -inOutCurrentPosition[1] - dy;

        S = S.scale(dx/ux, dy/uy, 1.0);
        T = T.translation((px/ux), (py/ux), 0);
        if ( inOutGlyphTextureTransform != null &&
             inOutGlyphTextureTransform.length > 0 ) {
            inOutGlyphTextureTransform[0] = Matrix4x4.copyOf(T.multiply(S));
        }

        // Next step for incremental algorithm
        inOutCurrentPosition[0] += newGlyph.getXSize()+1;

        return inOutFontMap;
    }

    /**
    This method generates a texture from a text to be displayed.
    @param label
    @param foreColor
    @param backColor
    @param size
    @return a new image containing a transparent representation of given text
    color, using a mono-spaced default font
    */
    public static RGBAImageUncompressed calculateLabelImage(
        String label,
        ColorRgb foreColor,
        ColorRgb backColor,
        int size)
    {
        //---------------------------------------------------------------------
        int foregroundColor;
        int backgroundColor;
        int r, g, b;

        r = (int)(foreColor.r * 255.0);
        g = (int)(foreColor.g * 255.0);
        b = (int)(foreColor.b * 255.0);
        foregroundColor = (0xFF << 24) + (r << 16) + (g << 8) + b;

        Typeface tf = Typeface.MONOSPACE;
        Paint foregroundPaintConfig = new Paint();
        foregroundPaintConfig.setAntiAlias(true);
        foregroundPaintConfig.setTextSize(size);
        foregroundPaintConfig.setColor(foregroundColor);
        foregroundPaintConfig.setTypeface(tf);

        Paint.FontMetrics fm = foregroundPaintConfig.getFontMetrics();

        //float fontHeight = (float)Math.ceil(Math.abs(fm.bottom) +
        //    Math.abs(fm.top));
        float fontAscent = (float)Math.ceil(Math.abs(fm.ascent));
        float fontDescent = (float)Math.ceil(Math.abs(fm.descent));

        //label = "H=" + fontHeight + ", A: " + fontAscent + ", D: " + fontDescent;

        //---------------------------------------------------------------------
        float widths[] = new float[label.length()];
        int n = foregroundPaintConfig.getTextWidths(label, 0, label.length(), widths);
        float l = 0;
        for ( int i = 0; i < n; i++ ) {
            l += widths[i];
        }

        //---------------------------------------------------------------------
        // Consider to use only ALPHA_8 - less memory required
        Bitmap bitmap;
        int w = (int)(l+1.0); //size * label.length();

        bitmap = Bitmap.createBitmap(
            w,
            (int)(fontAscent+fontDescent),
            Bitmap.Config.ARGB_8888);

        bitmap.eraseColor(0x00FF0000);
        Canvas gc = new Canvas(bitmap);

        if ( backColor != null ) {
            r = (int) (backColor.r * 255.0);
            g = (int) (backColor.g * 255.0);
            b = (int) (backColor.b * 255.0);
            backgroundColor = (0xFF << 24) + (r << 16) + (g << 8) + b;
            Paint backgroundPaintConfig = new Paint();
            backgroundPaintConfig.setAntiAlias(true);
            backgroundPaintConfig.setTextSize(size);
            backgroundPaintConfig.setColor(backgroundColor);
            backgroundPaintConfig.setTypeface(tf);
            gc.drawText(label, 0 + 1, fontAscent, backgroundPaintConfig);
            gc.drawText(label, 0, fontAscent + 1, backgroundPaintConfig);
            gc.drawText(label, 0 - 1, fontAscent, backgroundPaintConfig);
            gc.drawText(label, 0, fontAscent - 1, backgroundPaintConfig);
            gc.drawText(label, 0 + 1, fontAscent+1, backgroundPaintConfig);
            gc.drawText(label, 0 + 1, fontAscent-1, backgroundPaintConfig);
            gc.drawText(label, 0 - 1, fontAscent+1, backgroundPaintConfig);
            gc.drawText(label, 0 - 1, fontAscent-1, backgroundPaintConfig);
        }

        gc.drawText(label, 0, fontAscent, foregroundPaintConfig);

        //gc.drawLine(0, 0, w, 0, paint);
        //gc.drawLine(0, fontAscent-1, w, fontAscent-1, paint);
        //gc.drawLine(0, fontAscent+fontDescent-1, w, fontAscent+fontDescent-1, paint);

        //---------------------------------------------------------------------
        RGBAImageUncompressed img;
        img = new RGBAImageUncompressed();
        AndroidRGBAImageUncompressedRenderer.importFromAndroidBitmap(bitmap, img);
        return img;
    }

    private static void fillTransparent(RGBAImageUncompressed inOutFontMap) {
        int x;
        int y;
        RGBAPixel p = new RGBAPixel();
        p.r = 0;
        p.g = 0;
        p.b = 0;
        p.a = 0;

        for ( y = 0; y < inOutFontMap.getYSize(); y++ ) {
            for ( x = 0; x < inOutFontMap.getXSize(); x++ ) {
                inOutFontMap.putPixel(x, y, p);
            }
        }
    }

}
