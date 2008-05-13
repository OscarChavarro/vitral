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
import vsdk.toolkit.media.RGBImage;

public class JoglSimpleBodyRenderer extends JoglRenderer {
    private static void drawCommon(GL gl, SimpleBody b,
                            Camera c, RendererConfiguration q)
    {
        Image texture;
        Vector3D scale;
        Vector3D p;

        p = b.getPosition();
        scale = b.getScale();

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

        RGBImage nm = b.getNormalMapRgb();
        if ( q.isBumpMapSet() && (nm != null) ) {
            JoglImageRenderer.activateAsNormalMap(gl, nm, q);
        }

        gl.glTexEnvf(gl.GL_TEXTURE_ENV, gl.GL_TEXTURE_ENV_MODE, gl.GL_MODULATE);
    }

    public static void draw(GL gl, SimpleBody b,
                            Camera c, RendererConfiguration q)
    {
        gl.glPushMatrix();

        drawCommon(gl, b, c, q);

        JoglGeometryRenderer.draw(gl, b.getGeometry(), c, q);

        gl.glPopMatrix();
    }

    public static void drawWithVertexArrays(GL gl, SimpleBody b,
                            Camera c, RendererConfiguration q)
    {
        gl.glPushMatrix();

        drawCommon(gl, b, c, q);

        JoglGeometryRenderer.drawWithVertexArrays(gl, b.getGeometry(), c, q);

        gl.glPopMatrix();
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
