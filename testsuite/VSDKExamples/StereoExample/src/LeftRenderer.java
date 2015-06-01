//===========================================================================

import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;

import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.render.jogl.JoglRGBImageRenderer;

public class LeftRenderer implements GLEventListener {

    private JoglDrawingArea parent;
    private RGBImage resultImage;

    public LeftRenderer(JoglDrawingArea parent, RGBImage resultImage)
    {
        this.parent = parent;
        this.resultImage = resultImage;
    }

    public void display(GLAutoDrawable drawable) {
        GL2 gl= drawable.getGL().getGL2();

        gl.glClearColor(0.2f, 0.2f, 0.2f, 1);
        gl.glClear(gl.GL_COLOR_BUFFER_BIT);
        gl.glClear(gl.GL_DEPTH_BUFFER_BIT);

        parent.drawLeftModel(gl);

        JoglRGBImageRenderer.getImageJOGL(gl, resultImage);
    }

    public void displayChanged(GLAutoDrawable drawable, boolean a, boolean b) {
        ;
    }

    public void reshape(GLAutoDrawable drawable,
                        int x,
                        int y,
                        int width,
                        int height) {
        ;
    }

    public void init(GLAutoDrawable drawable) {
        ;
    }

    public void dispose(GLAutoDrawable drawable)
    {
        ;
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
