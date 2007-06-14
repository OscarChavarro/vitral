//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - November 27 2005 - Oscar Chavarro: Original base version              =
//= - February 15 2006 - Oscar Chavarro: Implemented true colorInDirection  =
//===========================================================================

package vsdk.toolkit.environment;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.media.RGBAImage;
import vsdk.toolkit.media.RGBAPixel;

public class CubemapBackground extends Background {
    /// Check the general attribute description in superclass Entity.
    public static final long serialVersionUID = 20060502L;

    private RGBAImage [] backgroundImages;
    private Camera camera;

    public CubemapBackground(Camera camera,
                             RGBAImage front,
                             RGBAImage right,
                             RGBAImage back,
                 RGBAImage left,
                 RGBAImage down,
                 RGBAImage up) {
        super();

        this.camera = camera;
        backgroundImages = new RGBAImage[6];
        backgroundImages[0] = front;
        backgroundImages[1] = right;
        backgroundImages[2] = back;
        backgroundImages[3] = left;
        backgroundImages[4] = down;
        backgroundImages[5] = up;
    }

    /**
    \todo
    ALL - method not implemented
    */
    public ColorRgb colorInDireccion(Vector3D d)
    {
        ColorRgb _color = new ColorRgb();
        RGBAPixel p;
        double theta, phi, u = 0, v = 0;
        RGBAImage i;

        phi = Math.acos(d.z);
        theta = Math.asin(d.y/Math.sin(phi));

        if ( phi > 3*Math.PI/4 ) {
            // Down
            i = backgroundImages[4];
    }
    else if ( phi > Math.PI/4 ) {
            // Front, right, back or left
            v = (phi - Math.PI/4) / (Math.PI/2);
            if ( theta > -Math.PI/4 && theta < Math.PI/4 && d.x > 0 ) {
                // Right
                i = backgroundImages[1];
                u = 1 - (theta+Math.PI/4)/(Math.PI/2);
        }
            else if ( theta > 0 -Math.PI/4 && theta < Math.PI/4 && d.x < 0 ) {
                // Left
                i = backgroundImages[3];
                u = (theta+Math.PI/4)/(Math.PI/2);
        }
            else if ( (theta >= Math.PI/4 && d.y > 0) ||
                      d.y > 0 && d.x < VSDK.EPSILON ) {
                // Front
                if ( theta < 0 ) theta = Math.PI/2;
                u = (theta - Math.PI/4) / (Math.PI/2);
                if ( d.x > 0 ) {
            u = (1 - u);
        }
                i = backgroundImages[0];
        }
        else {
                // Back
                theta *= -1;
                u = (theta - Math.PI/4) / (Math.PI/2);
                if ( d.x > 0 ) {
            u = (1 - u);
        }
                u = (1 - u);
                i = backgroundImages[2];
        }
    }
    else {
            // Up
            i = backgroundImages[5];
    }

        p = i.getPixel((int)(u*(i.getXSize()-1)), (int)(v*(i.getYSize()-1)));

        _color.r = ((double)VSDK.signedByte2unsignedInteger(p.r)) / 255.0;
        _color.g = ((double)VSDK.signedByte2unsignedInteger(p.g)) / 255.0;
        _color.b = ((double)VSDK.signedByte2unsignedInteger(p.b)) / 255.0;

        return _color;
    }

    public RGBAImage [] getImages()
    {
        return backgroundImages;
    }

    public Camera getCamera()
    {
        return camera;
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
