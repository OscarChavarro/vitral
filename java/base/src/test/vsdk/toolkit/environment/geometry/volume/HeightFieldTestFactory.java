package vsdk.toolkit.environment.geometry.volume;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;

/** Port of the cpp test helper `HeightFieldTestFactory`. */
public final class HeightFieldTestFactory
{
    private HeightFieldTestFactory()
    {
    }

    /** 5 x 5 grid of the plane h = 1 + x/2 + z/4 in the box [1,3]x[0,5]x[1,3]. */
    public static HeightFieldSolid createPlane()
    {
        HeightFieldSolid solid = new HeightFieldSolid(5, 5, new Vector3Dd(1, 0, 1), new Vector3Dd(3, 5, 3));
        for ( long z = 0; z < 5; ++z ) {
            for ( long x = 0; x < 5; ++x ) solid.setHeight(x, z, 1.0 + 0.5*x + 0.25*z);
        }
        solid.updateBlocks();
        return solid;
    }

    /** 17 x 17 grid of rolling hills in the box [1,15]x[0,6]x[1,15]. */
    public static HeightFieldSolid createHills()
    {
        HeightFieldSolid solid = new HeightFieldSolid(17, 17, new Vector3Dd(1, 0, 1), new Vector3Dd(15, 6, 15));
        for ( long z = 0; z < 17; ++z ) {
            for ( long x = 0; x < 17; ++x ) {
                solid.setHeight(x, z, 2.5 + 1.5*Math.sin(0.7*x)*Math.cos(0.45*z) + 0.3*Math.sin(1.9*x + 1.3*z));
            }
        }
        solid.updateBlocks();
        return solid;
    }
}
