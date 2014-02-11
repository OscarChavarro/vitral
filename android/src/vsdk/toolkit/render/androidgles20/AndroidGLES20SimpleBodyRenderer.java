//===========================================================================
package vsdk.toolkit.render.androidgles20;

// Java classes
import java.util.ArrayList;

// Android classes: OpenGL ES 2.0
import android.opengl.GLES20;

// VitralSDK classes
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.media.Image;
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.render.androidgles20.AndroidGLES20ImageRenderer;

public class AndroidGLES20SimpleBodyRenderer extends AndroidGLES20Renderer
{
    private static void drawCommon(SimpleBody b,
                            Camera c, RendererConfiguration q)
    {
        //-----------------------------------------------------------------
        Vector3D scale;
        Vector3D p;

        p = b.getPosition();
        scale = b.getScale();

        glLoadIdentity();
        glTranslated(p.x, p.y, p.z);
        //AndroidGLES20MatrixRenderer.activate(b.getRotation());
        glScaled(scale.x, scale.y, scale.z);

        //glColor3d(1, 1, 1);
        AndroidGLES20MaterialRenderer.activate(b.getMaterial());

        //-----------------------------------------------------------------
        Image texture;

        texture = b.getTexture();

        if ( texture == null ) {
            q.setTexture(false);
	}

        if ( q.isTextureSet() ) {
            // Define texture parameters, including for further local
            // textures activated within JoglGeometryRenderers
            // Activate global texture
            if ( (texture != null) ) {
                glEnable(GL_TEXTURE_2D);
                AndroidGLES20ImageRenderer.activate(texture);
                //GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                //    GL2.GL_GENERATE_MIPMAP, GL_TRUE);
                //GLES20.glTexEnvf(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE,
                //    GL2ES1.GL_MODULATE);
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, 
                    GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MAG_FILTER,
                    GLES20.GL_LINEAR);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
                    GLES20.GL_REPEAT);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
                    GLES20.GL_REPEAT);
            }
        }
        else {
            glDisable(GL_TEXTURE_2D);
        }

        //-----------------------------------------------------------------
        //RGBImage nm = b.getNormalMapRgb();
        //if ( q.isBumpMapSet() && (nm != null) ) {
        //    JoglImageRenderer.activateAsNormalMap(gl, nm, q);
        //}
    }

    public static void draw(SimpleBody b,
        Camera c, RendererConfiguration q)
    {
        RendererConfiguration q2 = q.clone();

        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        drawCommon(b, c, q2);
        AndroidGLES20GeometryRenderer.draw(b.getGeometry(), c, q2);
        glPopMatrix();
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
