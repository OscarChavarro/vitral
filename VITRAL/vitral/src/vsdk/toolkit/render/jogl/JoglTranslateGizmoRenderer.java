//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - March 16 2006 - Oscar Chavarro: Original base version                 =
//===========================================================================

package vsdk.toolkit.render.jogl;

import java.util.ArrayList;
import java.util.Iterator;

import javax.media.opengl.GL;

import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.common.Matrix4x4;
import vsdk.toolkit.common.QualitySelection;
import vsdk.toolkit.gui.TranslateGizmo;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.geometry.RayableObject;
import vsdk.toolkit.environment.geometry.Geometry;

public class JoglTranslateGizmoRenderer
{

    public static void draw(GL gl, TranslateGizmo gizmo, Vector3D position)
    {
        Matrix4x4 R;

        R = new Matrix4x4(); //gizmo.getTransformationMatrix());

        R.M[0][3] = position.x;
        R.M[1][3] = position.y;
        R.M[2][3] = position.z;

        gl.glDisable(gl.GL_LIGHTING);
        gl.glLineWidth(3);
        JoglMatrixRenderer.drawGL(gl, R);

        //-----------------------------------------------------------------
        QualitySelection q = new QualitySelection();
        ArrayList<RayableObject> things = gizmo.getElements();

        q.setSurfaces(true);
        q.setWires(false);
        q.setBoundingVolume(false);
        q.setTexture(false);
        q.setPoints(false);
        q.setBumpMap(false);
        q.setNormals(false);
        q.setShadingType(q.SHADING_TYPE_GOURAUD);

        gl.glEnable(gl.GL_LIGHTING);

        for ( Iterator i = things.iterator(); i.hasNext(); ) {
            RayableObject r = (RayableObject)i.next();
            Geometry g = r.getGeometry();

            if ( g != null ) {
                gl.glPushMatrix();
                gl.glLoadIdentity();
                position = r.getPosition();
                gl.glTranslated(position.x, position.y, position.z);
                JoglMatrixRenderer.activateGL(gl, r.getRotation());
                JoglMaterialRenderer.activate(gl, r.getMaterial());
                JoglGeometryRenderer.draw(gl, g, gizmo.getCamera(), q);
                gl.glPopMatrix();
        }
    }

        //-----------------------------------------------------------------
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
