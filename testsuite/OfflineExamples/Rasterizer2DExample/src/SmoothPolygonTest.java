//===========================================================================
//=-------------------------------------------------------------------------=
//= References:                                                             =
//= [FOLE1992] Foley, vanDam, Feiner, Hughes. "Computer Graphics,           =
//=          principles and practice" - second edition, Addison Wesley,     =
//=          1992.                                                          =
//===========================================================================

// Basic JDK classes
import java.io.File;

// VSDK classes
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.media.RGBPixel;
import vsdk.toolkit.environment.geometry.Polygon2D;
import vsdk.toolkit.render.Rasterizer2D;
import vsdk.toolkit.io.image.ImagePersistence;

public class SmoothPolygonTest
{
    public static void main (String[] args) {
        //-----------------------------------------------------------------
        RGBImage img = null;
        RGBPixel fillcolor = new RGBPixel();
        RGBPixel bordercolor = new RGBPixel();

        fillcolor.r = -1;
        fillcolor.g = 0;
        fillcolor.b = 0;

        bordercolor.r = -1;
        bordercolor.g = -1;
        bordercolor.b = 0;

        img = new RGBImage();
        img.init(640, 480);
        img.createTestPattern();

        //-----------------------------------------------------------------
        Polygon2D pol = new Polygon2D();

        pol.addVertex(70, 50, 1.0, 0.0, 0.0);
        pol.addVertex(400, 200, 0.0, 1.0, 0.0);
        pol.addVertex(100, 300, 0.0, 0.0, 1.0);
        pol.nextLoop();
        pol.addVertex(120, 150, 1.0, 1.0, 0.0);
        pol.addVertex(250, 150, 0.0, 1.0, 1.0);
        pol.addVertex(230, 220, 1.0, 0.0, 1.0);

/*
        pol.addVertex(320, 50, 1.0, 0.0, 0.0);
        pol.addVertex(20, 300, 0.0, 1.0, 0.0);
        pol.addVertex(620, 300, 0.0, 0.0, 1.0);
*/

        //-----------------------------------------------------------------
        Rasterizer2D.fillSmoothPolygon(img, pol);
        Rasterizer2D.drawPolygon(img, pol, bordercolor);

        ImagePersistence.exportBMP(new File("output.bmp"), img);
        System.out.println("Resulting image has been written to \"output.bmp\"");    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
