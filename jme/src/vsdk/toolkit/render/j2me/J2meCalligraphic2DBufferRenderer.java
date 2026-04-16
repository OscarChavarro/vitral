package vsdk.toolkit.render.j2me;

// J2ME Classes
import javax.microedition.lcdui.Graphics;

// VitralSDK Classes
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.media.Calligraphic2DBuffer;

public class J2meCalligraphic2DBufferRenderer extends J2meRenderer
{
    public static void draw(Graphics g, Calligraphic2DBuffer lineSet,
                            int xx0, int yy0, int dx, int dy)
    {
        g.setColor(0x000000);
        Vector3D p0 = new Vector3D();
        Vector3D p1 = new Vector3D();
        int x0, y0, x1, y1;
        int i;
        double xt = dx;
        double yt = dy;

        for ( i = 0; i < lineSet.getNumLines(); i++ ) {
            lineSet.get2DLine(i, p0, p1);
            x0 = (int)((xt-1)*((p0.x+1)/2));
            y0 = (int)((yt-1)*(1-((p0.y+1)/2)));
            x1 = (int)((xt-1)*((p1.x+1)/2));
            y1 = (int)((yt-1)*(1-((p1.y+1)/2)));
            g.drawLine(xx0 + x0, yy0 + y0, xx0 + x1, yy0 + y1);
        }
    }
}
