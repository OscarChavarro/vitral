package vsdk.toolkit.render.androidgles10;

// Android OpenGL ES classes
import android.opengl.GLES10;

// VSDK classes
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.environment.CubemapBackground;
import vsdk.toolkit.media.RGBAImageUncompressed;

public class AndroidGLES10CubemapBackgroundRenderer extends 
    AndroidGLES10BackgroundRenderer {

    public static void draw(CubemapBackground background) {
        //- General setup -----------------------------------------------------
        RGBAImageUncompressed images[] = background.getImages();

        RendererConfiguration q;
        q = new RendererConfiguration();
        q.setSurfaces(true);
        q.setTexture(true);
        q.setUseVertexColors(true);
        q.setShadingType(RendererConfiguration.SHADING_TYPE_NOLIGHT);
        AndroidGLES10Renderer.setRendererConfiguration(q);
        
        GLES10.glMatrixMode(GLES10.GL_PROJECTION);
        GLES10.glLoadIdentity();
        GLES10.glMatrixMode(GLES10.GL_MODELVIEW);
        GLES10.glLoadIdentity();

        AndroidGLES10CameraRenderer.activateCenter(background.getCamera());

        GLES10.glShadeModel(GLES10.GL_FLAT);
        GLES10.glDisable(GLES10.GL_BLEND);
        GLES10.glDisable(GLES10.GL_LIGHTING);
        GLES10.glDisable(GLES10.GL_DEPTH_TEST);
        GLES10.glFrontFace(GLES10.GL_CCW);
        GLES10.glActiveTexture(GLES10.GL_TEXTURE0);

        //---------------------------------------------------------------------

        // Front
        GLES10.glLoadIdentity();
        GLES10.glTranslatef(0.0f, 0.5f, 0.0f);
        GLES10.glRotatef(90.0f, 1.0f, 0.0f, 0.0f);
        GLES10.glScalef(1.001f, 1.001f, 1.001f);

        AndroidGLES10ImageRenderer.activate(images[0]);
        AndroidGLES10Renderer.activateDefaultTextureParameters();
        AndroidGLES10Renderer.drawUnitSquare();

        // Right
        GLES10.glLoadIdentity();
        GLES10.glTranslatef(0.5f, 0.0f, 0.0f);
        GLES10.glRotatef(-90.0f, 0.0f, 0.0f, 1.0f);
        GLES10.glRotatef(90.0f, 1.0f, 0.0f, 0.0f);
        GLES10.glScalef(1.001f, 1.001f, 1.001f);

        AndroidGLES10ImageRenderer.activate(images[1]);
        AndroidGLES10Renderer.activateDefaultTextureParameters();
        AndroidGLES10Renderer.drawUnitSquare();
        
        // Left
        GLES10.glLoadIdentity();
        GLES10.glTranslatef(-0.5f, 0.0f, 0.0f);
        GLES10.glRotatef(90.0f, 0.0f, 0.0f, 1.0f);
        GLES10.glRotatef(90.0f, 1.0f, 0.0f, 0.0f);
        GLES10.glScalef(1.001f, 1.001f, 1.001f);

        AndroidGLES10ImageRenderer.activate(images[3]);
        AndroidGLES10Renderer.activateDefaultTextureParameters();
        AndroidGLES10Renderer.drawUnitSquare();

        // Back
        GLES10.glLoadIdentity();
        GLES10.glTranslatef(0.0f, -0.5f, 0.0f);
        GLES10.glRotatef(180.0f, 0.0f, 0.0f, 1.0f);
        GLES10.glRotatef(90.0f, 1.0f, 0.0f, 0.0f);
        GLES10.glScalef(1.001f, 1.001f, 1.001f);

        AndroidGLES10ImageRenderer.activate(images[2]);
        AndroidGLES10Renderer.activateDefaultTextureParameters();
        AndroidGLES10Renderer.drawUnitSquare();

        // Down
        GLES10.glLoadIdentity();
        GLES10.glTranslatef(0.0f, 0.0f, -0.5f);
        GLES10.glRotatef(180.0f, 0.0f, 0.0f, 1.0f);
        GLES10.glScalef(1.001f, 1.001f, 1.001f);

        AndroidGLES10ImageRenderer.activate(images[5]);
        AndroidGLES10Renderer.activateDefaultTextureParameters();
        AndroidGLES10Renderer.drawUnitSquare();

        // Up
        GLES10.glLoadIdentity();
        GLES10.glTranslatef(0.0f, 0.0f, -0.5f);
        //GLES10.glRotatef(180.0f, 0.0f, 0.0f, 1.0f);
        GLES10.glScalef(1.001f, 1.001f, 1.001f);

        AndroidGLES10ImageRenderer.activate(images[4]);
        AndroidGLES10Renderer.activateDefaultTextureParameters();
        AndroidGLES10Renderer.drawUnitSquare();

        // End
        GLES10.glEnable(GLES10.GL_DEPTH_TEST);
    }
    
}
