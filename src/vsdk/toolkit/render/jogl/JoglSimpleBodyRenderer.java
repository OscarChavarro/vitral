//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - May 27 2007 - Oscar Chavarro: Original base version                   =
//===========================================================================

package vsdk.toolkit.render.jogl;

// JOGL clases
import javax.media.opengl.GL;

// VitralSDK classes
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.media.Image;

public class JoglSimpleBodyRenderer extends JoglRenderer {
    public static void draw(GL gl, SimpleBody b,
                            Camera c, RendererConfiguration q)
    {
        Image texture;
        Vector3D scale;
        Vector3D p;

        p = b.getPosition();
        scale = b.getScale();

        gl.glPushMatrix();
        gl.glTranslated(p.x, p.y, p.z);
        JoglMatrixRenderer.activate(gl, b.getRotation());
        gl.glScaled(scale.x, scale.y, scale.z);

        gl.glColor3d(1, 1, 1);
        JoglMaterialRenderer.activate(gl, b.getMaterial());

        texture = b.getTexture();

        if ( q.isTextureSet() && (texture != null) ) {
            gl.glEnable(gl.GL_TEXTURE_2D);
            JoglImageRenderer.activate(gl, texture);
        }
        else {
            gl.glDisable(gl.GL_TEXTURE_2D);
        }

        JoglGeometryRenderer.draw(gl, b.getGeometry(), c, q);

        gl.glPopMatrix();
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
