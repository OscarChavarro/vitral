import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;

import vsdk.toolkit.media.RGBImageUncompressed;
import vsdk.toolkit.render.jogl.Jogl2RGBImageUncompressedRenderer;

public class RightRenderer implements GLEventListener {

    private JoglDrawingArea parent;
    private RGBImageUncompressed resultImage;

    public RightRenderer(JoglDrawingArea parent, RGBImageUncompressed resultImage)
    {
        this.parent = parent;
        this.resultImage = resultImage;
    }

    public void display(GLAutoDrawable drawable) {
        GL2 gl= drawable.getGL().getGL2();

        gl.glClearColor(0.2f, 0.2f, 0.2f, 1);
        gl.glClear(gl.GL_COLOR_BUFFER_BIT);
        gl.glClear(gl.GL_DEPTH_BUFFER_BIT);

        parent.drawRightModel(gl);

        Jogl2RGBImageUncompressedRenderer.getImageJOGL(gl, resultImage);
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
