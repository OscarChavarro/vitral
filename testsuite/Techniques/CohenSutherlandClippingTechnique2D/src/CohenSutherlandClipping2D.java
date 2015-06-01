//===========================================================================

import vsdk.toolkit.processing.ComputationalGeometry;

// Java AWT/Swing classes
import java.awt.BorderLayout;
import java.awt.event.AdjustmentEvent;
import java.awt.GridLayout;
import java.awt.event.AdjustmentListener;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;

// JOGL classes
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;

// VSDK classes
import vsdk.toolkit.common.linealAlgebra.Vector3D;

/**
@author Andres Felipe Mejia (Paco el Caco)
 */
public class CohenSutherlandClipping2D implements 
        GLEventListener, AdjustmentListener {

    private static Vector3D min;
    private static Vector3D max;
    private Vector3D clipped0;
    private Vector3D clipped1;
    private static Vector3D p0;
    private static Vector3D p1;

    private JFrame frame;
    private JPanel panel;
    private JLabel label1;
    private JLabel label2;
    private JLabel label3;
    private JLabel label4;
    private JScrollBar sb1;
    private JScrollBar sb2;
    private JScrollBar sb3;
    private JScrollBar sb4;
    private GLCanvas canvas;

    private boolean clipped;

    public CohenSutherlandClipping2D(){
        
    }

    public void createGUI()
    {
        p0 = new Vector3D(0.85, 0.5, 0.0);
        p1 = new Vector3D(-0.85, -0.5, 0.0);
        min = new Vector3D(-0.8, -0.8, 0.0);
        max = new Vector3D(0.8, 0.8, 0.0);
        clipped0 = new Vector3D(p0);
        clipped1 = new Vector3D(p1);
        clipped = ComputationalGeometry.cohenSutherlandLineClipping2D(clipped0, clipped1, min, max);

        frame = new JFrame("VITRAL concept test - Cohen Sutherland 2D line clipping");
        frame.setSize(800, 600);
        panel = new JPanel(new GridLayout(2, 4));
        panel.setSize(800, 200);
        canvas = new GLCanvas();
        canvas.addGLEventListener(this);
        label1 = new JLabel("X0");
        label2 = new JLabel("Y0");
        label3 = new JLabel("X1");
        label4 = new JLabel("Y1");
        sb1 = new JScrollBar(JScrollBar.HORIZONTAL, -85, 1, -100, 100);
        sb1.setName("x0");
        sb1.addAdjustmentListener(this);
        sb2 = new JScrollBar(JScrollBar.HORIZONTAL, 50, 1, -100, 100);
        sb2.setName("y0");
        sb2.addAdjustmentListener(this);
        sb3 = new JScrollBar(JScrollBar.HORIZONTAL, -85, 1, -100, 100);
        sb3.setName("x1");
        sb3.addAdjustmentListener(this);
        sb4 = new JScrollBar(JScrollBar.HORIZONTAL, -50, 1, -100, 100);
        sb4.setName("y1");
        sb4.addAdjustmentListener(this);

        panel.add(label1);
        panel.add(sb1);
        panel.add(label3);
        panel.add(sb3);
        panel.add(label2);
        panel.add(sb2);
        panel.add(label4);
        panel.add(sb4);
        frame.add(canvas, BorderLayout.CENTER);
        frame.add(panel, BorderLayout.SOUTH);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    public static void main(String[] args) {
        CohenSutherlandClipping2D instance = new CohenSutherlandClipping2D();
        instance.createGUI();
    }

    public void init(GLAutoDrawable glad) {

    }

    public void dispose(GLAutoDrawable glad) {

    }

    public void display(GLAutoDrawable glad) {
        GL2 gl = glad.getGL().getGL2();

        gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        gl.glClear(gl.GL_COLOR_BUFFER_BIT | gl.GL_DEPTH_BUFFER_BIT);
        gl.glLoadIdentity();

        gl.glLineWidth(1.0f);
        gl.glPolygonMode(gl.GL_FRONT_AND_BACK, gl.GL_LINE);
        gl.glColor3d(0.0, 1.0, 0.0);
        gl.glBegin(gl.GL_QUADS);
            gl.glVertex2d(min.x, min.y);
            gl.glVertex2d(min.x, max.x);
            gl.glVertex2d(max.x, max.y);
            gl.glVertex2d(max.x, min.y);
        gl.glEnd();

        gl.glColor3d(0.0, 0.0, 1.0);
        gl.glBegin(gl.GL_LINES);
            gl.glVertex2d(p0.x, p0.y);
            gl.glVertex2d(p1.x, p1.y);
        gl.glEnd();

        if(clipped){
            gl.glColor3d(1.0, 0.0, 0.0);
            gl.glLineWidth(3.0f);
            gl.glBegin(gl.GL_LINES);
                gl.glVertex2d(clipped0.x, clipped0.y);
                gl.glVertex2d(clipped1.x, clipped1.y);
            gl.glEnd();
        }
    }

    public void reshape(GLAutoDrawable glad, int x, int y, int width, int height) {
        GL2 gl = glad.getGL().getGL2();
        gl.glViewport(0, 0, width, height);
        
    }

    public void adjustmentValueChanged(AdjustmentEvent e) {
        double value = (double) e.getValue() / 100;
        if ( ((JScrollBar)e.getAdjustable()).getName().equals("x0") ) {
            p0.x = value;
        }
        if ( ((JScrollBar)e.getAdjustable()).getName().equals("y0") ) {
            p0.y = value;
        }
        if ( ((JScrollBar)e.getAdjustable()).getName().equals("x1") ) {
            p1.x = value;
        }
        if ( ((JScrollBar)e.getAdjustable()).getName().equals("y1") ) {
            p1.y = value;
        }
        clipped0 = new Vector3D(p0);
        clipped1 = new Vector3D(p1);
        clipped = ComputationalGeometry.cohenSutherlandLineClipping2D(clipped0, clipped1, min, max);
        canvas.repaint();
    }
    
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
