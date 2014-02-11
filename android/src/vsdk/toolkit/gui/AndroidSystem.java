//===========================================================================

package vsdk.toolkit.gui;

// Android packages
import android.view.MotionEvent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;

// VSDK Classes
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.gui.PresentationElement;
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
        int size = 14;

        Typeface tf = Typeface.MONOSPACE;
        Paint paint = new Paint();
        paint.setAntiAlias( true );
        paint.setTextSize( size );
        paint.setColor( 0xffffffff );
        paint.setTypeface( tf );
        //Paint.FontMetrics fm = paint.getFontMetrics();

        // Consider to use only ALPHA_8 - less memory required
        Bitmap bitmap = Bitmap.createBitmap(120, 40, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(0x00FF0000);
        Canvas gc = new Canvas(bitmap);

        gc.drawText(label, 5, 15, paint);

        RGBAImage img;
        img = new RGBAImage();
        //img.init(120, 40);
        //img.createTestPattern();
        AndroidRGBAImageRenderer.importFromAndroidBitmap(bitmap, img);
        return img;
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
