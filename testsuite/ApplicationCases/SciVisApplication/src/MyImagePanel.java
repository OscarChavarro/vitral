//===========================================================================
//===========================================================================

import java.awt.Graphics;
import javax.swing.JPanel;

import vsdk.toolkit.media.Image;
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.render.awt.AwtRGBImageRenderer;

public class MyImagePanel extends JPanel
{
    private Image image;

    public MyImagePanel()
    {
        image = null;
    }

    public void setImage(Image img)
    {
        image = img;
    }

    public void paint(Graphics dc)
    {
        super.paint(dc);

        if ( image != null ) {
            AwtRGBImageRenderer.draw(dc, (RGBImage)image, 10, 10);
	}
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
