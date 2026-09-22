package vsdk.toolkit.gui.gizmo;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.scene.SimpleBody;

/**
Tessellates, in world space, the simple solids the gizmos are built with (the
shafts and heads of their axes, the small cubes at their tips and their plane
handles), so the technology-dependent renderers only have to send the resulting
points to the rasterizer.

Each element of a gizmo is a {@link SimpleBody} whose geometry is expressed in
its own local frame, and grows along its local +Z (see
{@link #localTransform(SimpleBody)}); every method here receives that local
frame as a matrix and returns points already transformed by it.

The primitives returned follow these conventions:

- A *strip* is a sequence of points for a triangle strip.
- A *fan* is a sequence of points for a triangle fan, with its center or apex
  as the first point.
*/
public class GizmoSolidTessellator {
    /// Number of subdivisions used around the axis of a revolution solid
    public static final int SLICES = 16;

    private static final double[] SLICE_COS = new double[SLICES + 1];
    private static final double[] SLICE_SIN = new double[SLICES + 1];

    /// The 8 corners of a box, indexed by (sx,sy,sz) sign combination as
    /// ((sx+1)/2)*4 + ((sy+1)/2)*2 + (sz+1)/2; each row is a face, given as a
    /// valid (non self-intersecting) strip of 4 corner indexes
    private static final int[][] BOX_FACES = {
        {0, 1, 2, 3}, {4, 5, 6, 7}, {0, 1, 4, 5}, {2, 3, 6, 7}, {0, 2, 4, 6}, {1, 3, 5, 7}
    };

    static {
        for ( int i = 0; i <= SLICES; i++ ) {
            double angle = 2*Math.PI*i/SLICES;

            SLICE_COS[i] = Math.cos(angle);
            SLICE_SIN[i] = Math.sin(angle);
        }
    }

    /**
    @param element element of a gizmo
    @return the matrix that takes the local frame of the element to world
    space
    */
    public static Matrix4x4d localTransform(SimpleBody element)
    {
        Vector3Dd position = element.getPosition();

        return new Matrix4x4d().translation(position).multiply(element.getRotation());
    }

    /**
    Tessellates the lateral surface of a shaft: a truncated cone from the
    local origin, growing along the local +Z axis.

    @param local local frame of the element
    @param baseRadius radius at the local origin
    @param topRadius radius at the local height
    @param height length of the shaft, along the local +Z axis
    @return strip with the lateral surface
    */
    public static Vector3Dd[] buildShaftStrip(Matrix4x4d local, double baseRadius,
                                              double topRadius, double height)
    {
        Vector3Dd[] strip = new Vector3Dd[(SLICES + 1)*2];

        for ( int i = 0; i <= SLICES; i++ ) {
            strip[2*i] = local.multiply(
                new Vector3Dd(baseRadius*SLICE_COS[i], baseRadius*SLICE_SIN[i], 0));
            strip[2*i + 1] = local.multiply(
                new Vector3Dd(topRadius*SLICE_COS[i], topRadius*SLICE_SIN[i], height));
        }
        return strip;
    }

    /**
    Tessellates the lateral surface of a cone whose base is at the local
    origin and whose apex is at the local height, over the +Z axis.

    @param local local frame of the element
    @param radius radius of the base of the cone
    @param height distance from the base to the apex
    @return fan with the apex as its first point
    */
    public static Vector3Dd[] buildConeSideFan(Matrix4x4d local, double radius, double height)
    {
        Vector3Dd[] fan = new Vector3Dd[SLICES + 2];

        fan[0] = local.multiply(new Vector3Dd(0, 0, height));
        for ( int i = 0; i <= SLICES; i++ ) {
            fan[i + 1] = local.multiply(
                new Vector3Dd(radius*SLICE_COS[i], radius*SLICE_SIN[i], 0));
        }
        return fan;
    }

    /**
    Tessellates the base of a cone built by {@link #buildConeSideFan}, with
    the opposite orientation, as it is seen from the other side.

    @param local local frame of the element
    @param radius radius of the base of the cone
    @return fan with the center of the base as its first point
    */
    public static Vector3Dd[] buildConeBaseFan(Matrix4x4d local, double radius)
    {
        Vector3Dd[] fan = new Vector3Dd[SLICES + 2];

        fan[0] = local.multiply(new Vector3Dd(0, 0, 0));
        for ( int i = 0; i <= SLICES; i++ ) {
            int k = SLICES - i;

            fan[i + 1] = local.multiply(
                new Vector3Dd(radius*SLICE_COS[k], radius*SLICE_SIN[k], 0));
        }
        return fan;
    }

    /**
    Tessellates a box centered at the local origin, as its 6 faces.

    @param local local frame of the element
    @param size lengths of the sides of the box, over each local axis
    @return one strip of 4 corners per face of the box
    */
    public static Vector3Dd[][] buildBoxFaceStrips(Matrix4x4d local, Vector3Dd size)
    {
        double hx = size.x()/2;
        double hy = size.y()/2;
        double hz = size.z()/2;
        Vector3Dd[] corners = new Vector3Dd[8];
        Vector3Dd[][] faces = new Vector3Dd[BOX_FACES.length][4];
        int index = 0;

        for ( int sx = -1; sx <= 1; sx += 2 ) {
            for ( int sy = -1; sy <= 1; sy += 2 ) {
                for ( int sz = -1; sz <= 1; sz += 2 ) {
                    corners[index++] = local.multiply(new Vector3Dd(sx*hx, sy*hy, sz*hz));
                }
            }
        }

        for ( int face = 0; face < BOX_FACES.length; face++ ) {
            for ( int corner = 0; corner < 4; corner++ ) {
                faces[face][corner] = corners[BOX_FACES[face][corner]];
            }
        }
        return faces;
    }

    /**
    Tessellates a rectangle centered at the local origin, over the local XY
    plane.

    @param local local frame of the element
    @param sizeX length of the rectangle over the local X axis
    @param sizeY length of the rectangle over the local Y axis
    @return strip with the 4 corners of the rectangle
    */
    public static Vector3Dd[] buildPlaneQuad(Matrix4x4d local, double sizeX, double sizeY)
    {
        double hx = sizeX/2;
        double hy = sizeY/2;

        return new Vector3Dd[] {
            local.multiply(new Vector3Dd(-hx, -hy, 0)),
            local.multiply(new Vector3Dd(hx, -hy, 0)),
            local.multiply(new Vector3Dd(-hx, hy, 0)),
            local.multiply(new Vector3Dd(hx, hy, 0))
        };
    }
}
