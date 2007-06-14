//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - March 15 2006 - Oscar Chavarro: Original base version                 =
//===========================================================================

package vsdk.toolkit.render.jogl;

import javax.media.opengl.GL;
import com.sun.opengl.cg.CgGL;
import com.sun.opengl.cg.CGprogram;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.common.Matrix4x4;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.geometry.Arrow;
import vsdk.toolkit.environment.geometry.Box;
import vsdk.toolkit.environment.geometry.Cone;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.ParametricCurve;
import vsdk.toolkit.environment.geometry.ParametricBiCubicPatch;
import vsdk.toolkit.environment.geometry.Sphere;
import vsdk.toolkit.environment.geometry.TriangleMesh;
import vsdk.toolkit.environment.geometry.TriangleMeshGroup;

public class JoglGeometryRenderer extends JoglRenderer 
{
    public static void prepareSurfaceQuality(GL gl, RendererConfiguration quality)
    {
        int shadingType = quality.getShadingType();

        switch ( shadingType ) {
          case RendererConfiguration.SHADING_TYPE_NOLIGHT:
            gl.glDisable(gl.GL_LIGHTING);
            gl.glShadeModel(gl.GL_FLAT);
            // Warning: Change with configured color for ambient lightning
            gl.glColor3d(1, 1, 1);
            break;
          case RendererConfiguration.SHADING_TYPE_FLAT:
            gl.glEnable(gl.GL_LIGHTING);
            gl.glShadeModel(gl.GL_FLAT);
            break;
          case RendererConfiguration.SHADING_TYPE_PHONG:
          case RendererConfiguration.SHADING_TYPE_GOURAUD: default:
            gl.glEnable(gl.GL_LIGHTING);
            gl.glShadeModel(gl.GL_SMOOTH);
            break;
        }
    }

    public static void drawMinMaxBox(GL gl, Geometry g, RendererConfiguration q)
    {
        double [] minmax;

        minmax = g.getMinMax();

        gl.glPushAttrib(gl.GL_LIGHTING_BIT);
        gl.glDisable(gl.GL_LIGHTING);
        gl.glDisable(gl.GL_TEXTURE_2D);
        // Warning: Change with configured color for bounding volume
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

    public static void drawSelectionCorners(GL gl, Geometry g, RendererConfiguration q)
    {
        double [] minmax;
        double borderPercent = 0.01;
        double linePercent = 0.25;
        minmax = g.getMinMax();

        Vector3D min, max, delta;
        min = new Vector3D(minmax[0], minmax[1], minmax[2]);
        max = new Vector3D(minmax[3], minmax[4], minmax[5]);
        delta = max.substract(min);
        min = min.substract(delta.multiply(borderPercent));
        max = max.add(delta.multiply(borderPercent));
        delta = delta.multiply(linePercent);

        gl.glPushAttrib(gl.GL_LIGHTING_BIT);
        gl.glDisable(gl.GL_LIGHTING);
        gl.glDisable(gl.GL_TEXTURE_2D);
        // Warning: Change with configured color for bounding volume
        gl.glColor3d(1, 1, 1);
        gl.glLineWidth(1.0f);
        gl.glBegin(gl.GL_LINES);

            gl.glVertex3d(min.x, min.y, min.z);
            gl.glVertex3d(min.x+delta.x, min.y, min.z);
            gl.glVertex3d(min.x, min.y, min.z);
            gl.glVertex3d(min.x, min.y+delta.y, min.z);
            gl.glVertex3d(min.x, min.y, min.z);
            gl.glVertex3d(min.x, min.y, min.z+delta.z);

            gl.glVertex3d(max.x, min.y, min.z);
            gl.glVertex3d(max.x-delta.x, min.y, min.z);
            gl.glVertex3d(max.x, min.y, min.z);
            gl.glVertex3d(max.x, min.y+delta.y, min.z);
            gl.glVertex3d(max.x, min.y, min.z);
            gl.glVertex3d(max.x, min.y, min.z+delta.z);

            gl.glVertex3d(min.x, max.y, min.z);
            gl.glVertex3d(min.x+delta.x, max.y, min.z);
            gl.glVertex3d(min.x, max.y, min.z);
            gl.glVertex3d(min.x, max.y-delta.y, min.z);
            gl.glVertex3d(min.x, max.y, min.z);
            gl.glVertex3d(min.x, max.y, min.z+delta.z);

            gl.glVertex3d(min.x, min.y, max.z);
            gl.glVertex3d(min.x+delta.x, min.y, max.z);
            gl.glVertex3d(min.x, min.y, max.z);
            gl.glVertex3d(min.x, min.y+delta.y, max.z);
            gl.glVertex3d(min.x, min.y, max.z);
            gl.glVertex3d(min.x, min.y, max.z-delta.z);

            gl.glVertex3d(max.x, max.y, min.z);
            gl.glVertex3d(max.x-delta.x, max.y, min.z);
            gl.glVertex3d(max.x, max.y, min.z);
            gl.glVertex3d(max.x, max.y-delta.y, min.z);
            gl.glVertex3d(max.x, max.y, min.z);
            gl.glVertex3d(max.x, max.y, min.z+delta.z);

            gl.glVertex3d(max.x, min.y, max.z);
            gl.glVertex3d(max.x-delta.x, min.y, max.z);
            gl.glVertex3d(max.x, min.y, max.z);
            gl.glVertex3d(max.x, min.y+delta.y, max.z);
            gl.glVertex3d(max.x, min.y, max.z);
            gl.glVertex3d(max.x, min.y, max.z-delta.z);

            gl.glVertex3d(min.x, max.y, max.z);
            gl.glVertex3d(min.x+delta.x, max.y, max.z);
            gl.glVertex3d(min.x, max.y, max.z);
            gl.glVertex3d(min.x, max.y-delta.y, max.z);
            gl.glVertex3d(min.x, max.y, max.z);
            gl.glVertex3d(min.x, max.y, max.z-delta.z);

            gl.glVertex3d(max.x, max.y, max.z);
            gl.glVertex3d(max.x-delta.x, max.y, max.z);
            gl.glVertex3d(max.x, max.y, max.z);
            gl.glVertex3d(max.x, max.y-delta.y, max.z);
            gl.glVertex3d(max.x, max.y, max.z);
            gl.glVertex3d(max.x, max.y, max.z-delta.z);

        gl.glEnd();

        gl.glPopAttrib();
    }

    public static void draw(GL gl, Geometry g, Camera c, RendererConfiguration q)
    {
        if ( g == null ) {
            VSDK.reportMessage(null, VSDK.WARNING,
                               "JoglGeometryRenderer.draw",
                               "null Geometry reference recieved");
            return;
        }

        if ( g instanceof Sphere ) {
            JoglSphereRenderer.draw(gl, (Sphere)g, c, q);
        }
        else if ( g instanceof Box ) {
            JoglBoxRenderer.draw(gl, (Box)g, c, q);
        }
        else if ( g instanceof Cone ) {
            JoglConeRenderer.draw(gl, (Cone)g, c, q);
        }
        else if ( g instanceof Arrow ) {
            JoglArrowRenderer.draw(gl, (Arrow)g, c, q);
        }
        else if ( g instanceof ParametricCurve ) {
            JoglParametricCurveRenderer.draw(gl, (ParametricCurve)g, c, q);
        }
        else if ( g instanceof ParametricBiCubicPatch ) {
            JoglParametricBiCubicPatchRenderer.draw(gl, (ParametricBiCubicPatch)g, c, q);
        }
        else if ( g instanceof TriangleMesh ) {
            JoglTriangleMeshRenderer.draw(gl, (TriangleMesh)g, q, false);
        }
        else if ( g instanceof TriangleMeshGroup ) {
            JoglTriangleMeshGroupRenderer.draw(gl, (TriangleMeshGroup)g,q);
        }
    }

    public static void activateNvidiaGpuParameters(GL gl, Geometry g,
        Camera camera, CGprogram vertexShader, CGprogram pixelShader)
    {
        Matrix4x4 MProjection;
        Matrix4x4 MModelviewGlobal;
        Matrix4x4 MModelviewLocal, MModelviewLocalIT, MCombined;
        double matrixarray[];

        MProjection = camera.calculateViewVolumeMatrix();
        MModelviewGlobal = camera.calculateTransformationMatrix();
        MModelviewLocal = MModelviewGlobal.multiply(
            JoglMatrixRenderer.importJOGL(gl, gl.GL_MODELVIEW_MATRIX));
        MCombined = MProjection.multiply(MModelviewLocal);
        MModelviewLocalIT = MModelviewLocal.inverse();
        MModelviewLocalIT.transpose();

        matrixarray = MCombined.exportToDoubleArrayRowOrder();
        CgGL.cgGLSetMatrixParameterdr(CgGL.cgGetNamedParameter(
            vertexShader, "modelViewProjectionLocal"),
            matrixarray, 0);

        matrixarray = MModelviewLocal.exportToDoubleArrayRowOrder();
        CgGL.cgGLSetMatrixParameterdr(CgGL.cgGetNamedParameter(
            vertexShader, "modelViewLocal"),
            matrixarray, 0);

        matrixarray = MModelviewLocalIT.exportToDoubleArrayRowOrder();
        CgGL.cgGLSetMatrixParameterdr(CgGL.cgGetNamedParameter(
            vertexShader, "modelViewLocalIT"),
            matrixarray, 0);
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
