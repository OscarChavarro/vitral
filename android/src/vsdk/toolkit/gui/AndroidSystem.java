//===========================================================================

package vsdk.toolkit.gui;

// Android packages
import android.view.MotionEvent;

// VSDK Classes
import vsdk.toolkit.gui.PresentationElement;

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
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
