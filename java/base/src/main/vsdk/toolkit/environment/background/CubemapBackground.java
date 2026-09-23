package vsdk.toolkit.environment.background;
import java.io.Serial;

import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.element.Ray;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.element.RayHit;
import vsdk.toolkit.environment.geometry.volume.Box;
import vsdk.toolkit.media.RGBAImageUncompressed;

public class CubemapBackground extends Background {
    @Serial private static final long serialVersionUID = 20060502L;

    private RGBAImageUncompressed [] images;
    private Camera camera;
    private Box boundingCube = null;

    public CubemapBackground(Camera camera,
                             RGBAImageUncompressed front,
                             RGBAImageUncompressed right,
                             RGBAImageUncompressed back,
                             RGBAImageUncompressed left,
                             RGBAImageUncompressed down,
                             RGBAImageUncompressed up) {
        super();

        this.camera = camera;
        images = new RGBAImageUncompressed[6];
        images[0] = front;
        images[1] = right;
        images[2] = back;
        images[3] = left;
        images[4] = down;
        images[5] = up;
        boundingCube = new Box(1, 1, 1);
    }

    /**
    @param d
    @return color as viewed in given direction
    */
    @Override
    public ColorRgb colorInDireccion(Vector3Dd d)
    {
        double u;
        double v;
        RGBAImageUncompressed img;

        d = d.normalized();
        Ray r = new Ray(new Vector3Dd(0, 0, 0), d);
        RayHit hit = new RayHit();
        if ( !boundingCube.doIntersectionFirstHit(r, hit) ) {
            return new ColorRgb();
        }
        int plane = classifyPlane(hit.normal);

        u = 1 - hit.u;
        v = 1 - hit.v;
        switch ( plane ) {
          case 1: // Top
            img = images[5];
            u = 1 - hit.v;
            v = hit.u;
            break;
          case 2: // Down
            img = images[4];
            u = hit.v;
            v = 1 - hit.u;
            break;
          case 3: // Front
            img = images[0];
            break;
          case 4: // Back
            img = images[2];
            break;
          case 5: // Right
            img = images[1];
            break;
          default: // Left
            img = images[3];
            break;
        }

        return img.getColorRgbBiLinear(u, v);
    }

    private int classifyPlane(Vector3Dd normal)
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

    public RGBAImageUncompressed [] getImages()
    {
        return images;
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
