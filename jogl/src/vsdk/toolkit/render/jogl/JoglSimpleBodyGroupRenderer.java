//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - May 27 2007 - Oscar Chavarro: Original base version                   =
//===========================================================================

package vsdk.toolkit.render.jogl;

// Java classes
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

// JOGL classes
import javax.media.opengl.GL2;

// VitralSDK classes
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.environment.scene.SimpleBodyGroup;
import vsdk.toolkit.media.Image;

public class JoglSimpleBodyGroupRenderer extends JoglRenderer {
    public static void draw(GL2 gl, SimpleBodyGroup b,
                            Camera c, RendererConfiguration q)
    {
        Image texture;
        Vector3D scale;
        Vector3D p;
        int i;
        SimpleBody gi;
        RendererConfiguration internalQuality;

        try {
            internalQuality = q.clone();
            internalQuality.setSelectionCorners(false);
            internalQuality.setBoundingVolume(false);

            ArrayList<SimpleBody> sublist;
            sublist = b.getBodies();

            gl.glPushMatrix();
            p = b.getPosition();
            gl.glTranslated(p.x, p.y, p.z);
            JoglMatrixRenderer.activate(gl, b.getRotation());
            scale = b.getScale();
            gl.glScaled(scale.x, scale.y, scale.z);

            for (i = 0; i < sublist.size(); i++) {
                gi = sublist.get(i);
                JoglSimpleBodyRenderer.draw(gl, gi, c, internalQuality);
            }

            if (q.isSelectionCornersSet()) {
                JoglGeometryRenderer.drawSelectionCorners(gl, b.getMinMax(), q);
            }
            if (q.isBoundingVolumeSet()) {
                JoglGeometryRenderer.drawMinMaxBox(gl, b.getMinMax(), q);
            }
            gl.glPopMatrix();
        } catch (CloneNotSupportedException ex) {

        }

    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
