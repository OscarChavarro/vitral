//===========================================================================
package vsdk.toolkit.render.androidgles20;

// Java classes
import java.util.ArrayList;

// Android classes: OpenGL ES 2.0
import android.opengl.GLES20;

// VitralSDK classes
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.media.Image;

public class AndroidGLES20SimpleBodyRenderer extends AndroidGLES20Renderer
{
    private static void drawCommon(SimpleBody b,
                            Camera c, RendererConfiguration q)
    {
        //-----------------------------------------------------------------
        // Activate current body transformation matrix
        Matrix4x4 R = b.getRotation();
        Matrix4x4 S = new Matrix4x4();
        Matrix4x4 T = new Matrix4x4();
        Vector3D p = b.getPosition();
        Vector3D s = b.getScale();

        S.scale(s);
        T.translation(p);
        Matrix4x4 M;

        M = T.multiply(R.multiply(S));
        glLoadIdentity();
        AndroidGLES20MatrixRenderer.activate(M);

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
                AndroidGLES20ImageRenderer.activate(texture);
                
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                AndroidGLES20ImageRenderer.activate(texture);
                activateDefaultTextureParameters();
            }
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
