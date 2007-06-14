//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - March 15 2006 - Oscar Chavarro: Original base version                 =
//===========================================================================

package vsdk.toolkit.render.jogl;

import javax.media.opengl.GL;

import vsdk.toolkit.common.QualitySelection;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.geometry.Arrow;
import vsdk.toolkit.environment.geometry.Box;
import vsdk.toolkit.environment.geometry.Cone;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.ParametricCubicCurve;
import vsdk.toolkit.environment.geometry.ParametricBiCubicPatch;
import vsdk.toolkit.environment.geometry.Sphere;
import vsdk.toolkit.environment.geometry.TriangleMesh;
import vsdk.toolkit.environment.geometry.TriangleMeshGroup;

public class JoglGeometryRenderer
{
    public static void drawMinMaxBox(GL gl, Geometry g, QualitySelection q)
    {
        double [] minmax;

        minmax = g.getMinMax();

        gl.glPushAttrib(gl.GL_LIGHTING_BIT);
        gl.glDisable(gl.GL_LIGHTING);
        gl.glColor3d(1, 1, 0);
        gl.glLineWidth(1.0f);
        gl.glBegin(gl.GL_LINE_LOOP);
            gl.glVertex3d(minmax[0], minmax[1], minmax[5]); // 6
            gl.glVertex3d(minmax[3], minmax[1], minmax[5]); // 5
            gl.glVertex3d(minmax[3], minmax[4], minmax[5]); // 8
            gl.glVertex3d(minmax[0], minmax[4], minmax[5]); // 7
            gl.glVertex3d(minmax[0], minmax[1], minmax[5]); // 6
            gl.glVertex3d(minmax[0], minmax[1], minmax[2]); // 1
            gl.glVertex3d(minmax[0], minmax[4], minmax[2]); // 2
            gl.glVertex3d(minmax[0], minmax[4], minmax[5]); // 7
        gl.glEnd();

        gl.glBegin(gl.GL_LINE_LOOP);
            gl.glVertex3d(minmax[3], minmax[1], minmax[2]); // 4
            gl.glVertex3d(minmax[3], minmax[4], minmax[2]); // 3
            gl.glVertex3d(minmax[0], minmax[4], minmax[2]); // 2
            gl.glVertex3d(minmax[0], minmax[1], minmax[2]); // 1
            gl.glVertex3d(minmax[3], minmax[1], minmax[2]); // 4
            gl.glVertex3d(minmax[3], minmax[1], minmax[5]); // 5
            gl.glVertex3d(minmax[3], minmax[4], minmax[5]); // 8
            gl.glVertex3d(minmax[3], minmax[4], minmax[2]); // 3
        gl.glEnd();

        gl.glPopAttrib();
    }

    public static int draw(GL gl, Geometry g, Camera c, QualitySelection q)
    {
        String geometryType = g.getClass().getName();
        int triangles = 0;

        if ( geometryType == "vsdk.toolkit.environment.geometry.Sphere" ) {
            triangles = JoglSphereRenderer.draw(gl, (Sphere)g, c, q);
    }
        else if ( geometryType == "vsdk.toolkit.environment.geometry.Box" ) {
            triangles = JoglBoxRenderer.draw(gl, (Box)g, c, q);
    }
        else if ( geometryType == "vsdk.toolkit.environment.geometry.Cone" ) {
            triangles = JoglConeRenderer.draw(gl, (Cone)g, c, q);
    }
        else if ( geometryType == "vsdk.toolkit.environment.geometry.Arrow" ) {
            triangles = JoglArrowRenderer.draw(gl, (Arrow)g, c, q);
        }
        else if ( geometryType == "vsdk.toolkit.environment.geometry.ParametricCubicCurve" ) {
        JoglParametricCubicCurveRenderer.draw(gl, (ParametricCubicCurve)g, c, q);
        }
        else if ( geometryType == "vsdk.toolkit.environment.geometry.ParametricBiCubicPatch" ) {
        triangles = JoglParametricBiCubicPatchRenderer.draw(gl, (ParametricBiCubicPatch)g, c, q);
        }
        else if ( geometryType == "vsdk.toolkit.environment.geometry.TriangleMesh" ) {
            triangles = JoglTriangleMeshRenderer.draw(gl, (TriangleMesh)g, q, false);
        }
        else if ( geometryType == "vsdk.toolkit.environment.geometry.TriangleMeshGroup" ) {
            triangles = JoglTriangleMeshGroupRenderer.draw(gl, (TriangleMeshGroup)g,q);
        }
        return triangles;
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
