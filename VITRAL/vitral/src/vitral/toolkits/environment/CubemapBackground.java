//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - November 27 2005 - Oscar Chavarro: Original base version              =
//===========================================================================

package vitral.toolkits.environment;

import vitral.toolkits.common.ColorRgb;
import vitral.toolkits.common.Vector3D;
import vitral.toolkits.environment.Camera;
import vitral.toolkits.media.RGBAImage;

public class CubemapBackground extends Background {
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
        _color.r = 0;
        _color.g = 1;
        _color.b = 0;
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
