//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - March 15 2006 - Oscar Chavarro: Original base version                 =
//===========================================================================

package vsdk.toolkit.render.jogl;

import javax.media.opengl.GL;

import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.Sphere;

public class JoglGeometryRenderer
{
    public static void draw(GL gl, Geometry g)
    {
        String geometryType = g.getClass().getName();

        if ( geometryType == "vsdk.toolkit.environment.geometry.Sphere" ) {
            JoglSphereRenderer.draw(gl, (Sphere)g);
    }
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
