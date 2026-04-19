package vsdk.toolkit.environment;
import java.io.Serial;

import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.common.Ray;
import vsdk.toolkit.environment.geometry.RayHit;
import vsdk.toolkit.environment.geometry.volume.Box;
import vsdk.toolkit.media.RGBAImage;

public class CubemapBackground extends Background {
    @Serial private static final long serialVersionUID = 20060502L;

    private RGBAImage [] backgroundImages;
    private Camera camera;
    private Box boundingCube = null;

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
        boundingCube = new Box(1, 1, 1);
    }

    /**
    @param d
    @return color as viewed in given direction
    */
    @Override
    public ColorRgb colorInDireccion(Vector3D d)
    {
        double u;
        double v;
        RGBAImage img;

        d = d.normalized();
        Ray r = new Ray(new Vector3D(0, 0, 0), d);
        RayHit hit = new RayHit();
        if ( !boundingCube.doIntersection(r, hit) ) {
            return new ColorRgb();
        }
        int plane = classifyPlane(hit.n);

        u = 1 - hit.u;
        v = 1 - hit.v;
        switch ( plane ) {
          case 1: // Top
            img = backgroundImages[5];
            u = 1 - hit.v;
            v = hit.u;
            break;
          case 2: // Down
            img = backgroundImages[4];
            u = hit.v;
            v = 1 - hit.u;
            break;
          case 3: // Front
            img = backgroundImages[0];
            break;
          case 4: // Back
            img = backgroundImages[2];
            break;
          case 5: // Right
            img = backgroundImages[1];
            break;
          default: // Left
            img = backgroundImages[3];
            break;
        }

        return img.getColorRgbBiLinear(u, v);
    }

    private int classifyPlane(Vector3D normal)
    {
        double ax = Math.abs(normal.x());
        double ay = Math.abs(normal.y());
        double az = Math.abs(normal.z());

        if ( az >= ax && az >= ay ) {
            return normal.z() >= 0 ? 1 : 2;
        }
        if ( ay >= ax ) {
            return normal.y() >= 0 ? 3 : 4;
        }
        return normal.x() >= 0 ? 5 : 6;
    }

    public RGBAImage [] getImages()
    {
        return backgroundImages;
    }

    public Camera getCamera()
    {
        return camera;
    }

    public void setCamera(Camera camera)
    {
        this.camera = camera;
    }
}
