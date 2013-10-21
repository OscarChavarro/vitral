//===========================================================================

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Image;

public class About {
    private static final String copyright =
        "MIDlet program example using VitralSDK.\n" + 
        "Pontificia Universidad Javeriana Bogota - Oscar Chavarro.\n" + 
        "http://vitral.sf.net";

    public About() {
        ;
    }

    /**
    Put up the About box and when the user click
    ok return to the previous screen.
    @param display The <code>Display</code> to return to when the about screen
    is dismissed and use as GUI context.
    */
    public static void showAbout(Display display) {
        Alert alert = new Alert("About Vitral SDK MIDlet");
        alert.setTimeout(Alert.FOREVER);

        if ( display.numColors() > 2 ) {
            String icon =
                (display.isColor()) ? "/jed8.png" : "/jed2.png";

            try {
                Image image = Image.createImage(icon);
                alert.setImage(image);
            }
            catch ( java.io.IOException x ) {
                // just don't append the image.
                ;
            }
        }

        // Add the copyright
        alert.setString(copyright);

        display.setCurrent(alert);
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
