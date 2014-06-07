//===========================================================================

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
import vsdk.toolkit.media.RGBAImage;
import vsdk.toolkit.render.android.AndroidRGBAImageRenderer;

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

    public static RGBAImage calculateLabelImage(String label, ColorRgb color)
    {
        return calculateLabelImage(label, color, 14);
    }

    /**
    This method generates a texture from a text to be displayed.
    @param label
    @param color
    @param size
    @return a new image containing a transparent representation of given text
    color, using a monospaced default font
    */
    public static RGBAImage calculateLabelImage(String label, ColorRgb color, int size)
    {
        //---------------------------------------------------------------------
        int c;
        int r, g, b;

        r = (int)(color.r * 255.0);
        g = (int)(color.g * 255.0);
        b = (int)(color.b * 255.0);
        c = (0xFF << 24) + (r << 16) + (g << 8) + b; 

        Typeface tf = Typeface.MONOSPACE;
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setTextSize(size);
        paint.setColor(c);
        paint.setTypeface(tf);

        Paint.FontMetrics fm = paint.getFontMetrics();

        //float fontHeight = (float)Math.ceil(Math.abs(fm.bottom) + 
        //    Math.abs(fm.top));
        float fontAscent = (float)Math.ceil(Math.abs(fm.ascent));
        float fontDescent = (float)Math.ceil(Math.abs(fm.descent));

        //label = "H=" + fontHeight + ", A: " + fontAscent + ", D: " + fontDescent;

        //---------------------------------------------------------------------
        float widths[] = new float[label.length()];
        int n = paint.getTextWidths(label, 0, label.length(), widths);
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

        gc.drawText(label, 0, fontAscent, paint);

        //gc.drawLine(0, 0, w, 0, paint);
        //gc.drawLine(0, fontAscent-1, w, fontAscent-1, paint);
        //gc.drawLine(0, fontAscent+fontDescent-1, w, fontAscent+fontDescent-1, paint);

        //---------------------------------------------------------------------
        RGBAImage img;
        img = new RGBAImage();
        AndroidRGBAImageRenderer.importFromAndroidBitmap(bitmap, img);
        return img;
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
