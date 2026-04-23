package vsdk.toolkit.render.jogl;

// Java classes
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

// JOGL classes
import com.jogamp.opengl.GL2;

// VitralSDK classes
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.environment.scene.SimpleBodyGroup;
import vsdk.toolkit.media.Image;

public class Jogl2SimpleBodyGroupRenderer extends Jogl2Renderer {
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
            gl.glTranslated(p.x(), p.y(), p.z());
            Jogl2MatrixRenderer.activate(gl, b.getRotation());
            scale = b.getScale();
            gl.glScaled(scale.x(), scale.y(), scale.z());

            for (i = 0; i < sublist.size(); i++) {
                gi = sublist.get(i);
                Jogl2SimpleBodyRenderer.draw(gl, gi, c, internalQuality);
            }

            if (q.isSelectionCornersSet()) {
                Jogl2GeometryRenderer.drawSelectionCorners(gl, b.getMinMax(), q);
            }
            if (q.isBoundingVolumeSet()) {
                Jogl2GeometryRenderer.drawMinMaxBox(gl, b.getMinMax(), q);
            }
            gl.glPopMatrix();
        } catch (CloneNotSupportedException ex) {

        }

    }
}
