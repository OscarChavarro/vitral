//===========================================================================
//===========================================================================

import java.awt.Graphics;
import javax.swing.JPanel;

import vsdk.toolkit.media.Image;
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.render.awt.AwtRGBImageRenderer;

// Application classes
import scivis.Study;
import scivis.TimeTake;

public class ViewerPanel2DSwing extends ViewerPanel
{
    private Image image;
    private int pendingCycles = -1;

    public ViewerPanel2DSwing(SciVisApplication parent, PanelManager container)
    {
        super(parent, container);
        image = null;
        this.parent = parent;
    }

    private void cycleSlices(Graphics dc)
    {
        if ( parent.study == null ) {
            return;
        }
        parent.currentSlice = 0;
        parent.currentTimeTake = 0;

        //-----------------------------------------------------------------
        Image img = null;
        int i;
        TimeTake timeTake = parent.study.getTimeTake(0);

        img = parent.study.getSliceImageAt(parent.currentTimeTake, pendingCycles);
        AwtRGBImageRenderer.draw(dc, (RGBImage)img, 10, 10);
    }

    public void setCyclePending(boolean flag)
    {
        if ( flag == true ) {
            TimeTake timeTake = parent.study.getTimeTake(0);
            pendingCycles = timeTake.getNumSlices() - 1;
        }
        else {
            pendingCycles = -1;
        }
    }

    public void setImage(Image img)
    {
        image = img;
    }

    public void paint(Graphics dc)
    {
        super.paint(dc);

        if ( pendingCycles >= 0 ) {
            cycleSlices(dc);
            pendingCycles--;
            repaint();
        }
        else {
            if ( image != null ) {
                AwtRGBImageRenderer.draw(dc, (RGBImage)image, 10, 10);
            }
        }
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
