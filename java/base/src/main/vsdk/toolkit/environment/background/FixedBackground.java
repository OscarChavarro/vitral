package vsdk.toolkit.environment.background;
import java.io.Serial;

import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.media.RGBAImageUncompressed;

public class FixedBackground extends Background {
    @Serial private static final long serialVersionUID = 20060502L;

    private RGBAImageUncompressed backgroundImage;
    private Camera camera;

    public FixedBackground(Camera camera, RGBAImageUncompressed image) {
        super();

        this.camera = camera;
        backgroundImage = image;
    }

    public void setImage(RGBAImageUncompressed image)
    {
        backgroundImage = image;
    }

    public RGBAImageUncompressed getImage()
    {
        return backgroundImage;
    }

    /**
    BUG: not working math!
    @param d
    @return color as viewed in given direction
    */
    @Override
    public ColorRgb colorInDireccion(Vector3Dd d)
    {
        return null;
/*
        InfinitePlane plane = camera.calculateNearPlane();
        Ray r = new Ray(camera.getPosition(), d);
        ColorRgb color = new ColorRgb();
        double u, v;
        Vector3Dd p;
        Vector3Dd left = camera.getLeft();
        Vector3Dd up = camera.getUp();
        Vector3Dd rel;
        double near = camera.getNearPlaneDistance();
        Vector3Dd front = camera.getFront();

        if ( plane.doIntersectionWithNegative(r) ) {
            p = r.origin.add(d.multiply(r.t));
            rel = p.substract(front.multiply(near));
            u = rel.dotProduct(left);
            v = rel.dotProduct(up);
            color.r = 1;
            color.g = 0;
            color.b = 0;
            if ( u >= -1 && u <= 1 && v >= -1 && v <= 1 ) {
                return backgroundImage.getColorRgbBiLinear(u, v);
            }
        }

        return color;
*/
    }


    public Camera getCamera()
    {
        return camera;
    }
}
