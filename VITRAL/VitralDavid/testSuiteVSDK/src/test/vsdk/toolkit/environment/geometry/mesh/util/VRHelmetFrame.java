package testsuite.frames;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;

import net.java.games.jogl.GL;
import net.java.games.jogl.GLU;
import net.java.games.jogl.GLCanvas;
import net.java.games.jogl.GLCapabilities;
import net.java.games.jogl.GLEventListener;
import net.java.games.jogl.GLDrawable;
import net.java.games.jogl.GLDrawableFactory;

import vitral.toolkits.environment.Camera;
import vitral.toolkits.gui.*;
import vitral.toolkits.gui.adapters.ControllerAdapter;
import vitral.toolkits.visual.jogl.JoglCameraRenderer;
import vitral.toolkits.visual.jogl.JoglScene;
import vitral.toolkits.visual.jogl.SceneController;
import vitral.toolkits.common.Vector3D;

import java.util.*;

/**
 *
 * @author usuario
 */
public class VRHelmetFrame extends JFrame implements ActionListener, GLEventListener
{
    private JFrame frameConfig=new JFrame("Configuracion");
    private JMenuItem miConfig=new JMenuItem("Configuracion");
    private JButton bAceptar=new JButton("Aceptar");
    
    private JMenuBar mb=new JMenuBar();;
    private JMenu mFile=new JMenu("Archivo");;
    private JMenu mHelp=new JMenu("Ayuda");;
    private JMenuItem miSalir=new JMenuItem("Salir");
    private GLCanvas canvas;
    
    private GL gl;
    private ControllerAdapter listenAdapter;
    private Camera cameraL;
    private Camera cameraR;
    private CameraController controlCam;
    private JoglScene escena;
    
    private ControlPanel controls=new ControlPanel();
    private float dEye;
    
    private class Swaper implements Runnable
    {
        public void run()
        {
            while(true)
            {
                canvas.swapBuffers();
                canvas.repaint();
                try
                {
                    Thread.sleep(100);
                }
                catch(InterruptedException ie)
                {
                    
                }
            }
        }
    }
    
    public VRHelmetFrame(JoglScene e)
    {
        super("VITRAL (shift+mover mouse para recuperar el puntero)");
        
        this.addWindowListener
        (
            new WindowAdapter()
            {
                public void windowClosing(WindowEvent e)
                {
                    System.exit(0);
                }
            }
        );
        
        escena=e;
        
        cameraL=new Camera();
        cameraR=new Camera();
        controlCam=new CameraControllerGravZero(cameraL);
        listenAdapter=new ControllerAdapter(controlCam);
        
        GLCapabilities capabilities = new GLCapabilities();
        capabilities.setStencilBits(8);
        canvas = GLDrawableFactory.getFactory().createGLCanvas(capabilities);
        
        canvas.addGLEventListener(this);
        listenAdapter.register(canvas);
        
        GL gl=canvas.getGL();
        GLU glu=canvas.getGLU();
        
        this.gl=gl;
        
        miSalir.addActionListener(this);
        mFile.getPopupMenu().setLightWeightPopupEnabled(false);
        
        mb.add(mFile);
        mb.add(mHelp);
        
        this.setJMenuBar(mb);
        this.getContentPane().add(canvas, BorderLayout.CENTER);
        
        armarFrame(escena.getSceneController());
        
        armarConfig();
        
        mFile.addSeparator();
        mFile.add(miSalir);
        
        Swaper s=new Swaper();
        Thread swap=new Thread(s);
        swap.start();
        
        this.setBounds(0,0, 800, 600);
        this.setVisible(true);
    }
    
    private void armarConfig()
    {
        frameConfig.getContentPane().add(controls, BorderLayout.CENTER);
        
        frameConfig.addWindowListener
        (
            new WindowAdapter()
            {
                public void windowClosing(WindowEvent e)
                {
                    frameConfig.setVisible(false);
                }
            }
        );
        
        miConfig.addActionListener(this);
        bAceptar.addActionListener(this);
        
        mFile.add(miConfig);
        
        JPanel PBotonSouth=new JPanel();
        PBotonSouth.add(bAceptar);
        
        frameConfig.getContentPane().add(bAceptar, BorderLayout.SOUTH);
        frameConfig.pack();
    }
    
    private void armarFrame(SceneController controlador)
    {
        if(controlador==null)
        {
            return;
        }
        
        if(controlador.getControlPaneNorth()!=null)
        {
            this.getContentPane().add(controlador.getControlPaneNorth(), BorderLayout.NORTH);
        }
        
        if(controlador.getControlPaneSouth()!=null)
        {
            this.getContentPane().add(controlador.getControlPaneSouth(), BorderLayout.SOUTH);
        }
        
        if(controlador.getControlPaneEast()!=null)
        {
            this.getContentPane().add(controlador.getControlPaneEast(), BorderLayout.EAST);
        }
        
        if(controlador.getControlPaneWest()!=null)
        {
            this.getContentPane().add(controlador.getControlPaneWest(), BorderLayout.WEST);
        }
        
        if(controlador.getFileMenus()!=null)
        {
            for(int i=0; i<controlador.getFileMenus().size(); i++)
            {
                mFile.add(controlador.getFileMenus().get(i));
            }
        }

        if(controlador.getHelpMenus()!=null)
        {
            for(int i=0; i<controlador.getHelpMenus().size(); i++)
            {
                mHelp.add(controlador.getHelpMenus().get(i));
            }
        }

        if(controlador.getMenus()!=null)
        {
            for(int i=0; i<controlador.getMenus().size(); i++)
            {
                mb.add(controlador.getMenus().get(i));
            }
        }
    }
    
    public void display(GLDrawable drawable)
    {
        GL gl = drawable.getGL();
        GLU glu = drawable.getGLU();

        cameraR.setPosition(cameraL.getPosition());
        cameraR.setFocusedPosition(cameraL.getFocusedPosition());
        cameraR.setUp(cameraL.getUp());
        cameraR.translate(dEye,0,0);
        
        gl.glEnable(gl.GL_DEPTH_TEST);

        gl.glColorMask(true, true, true, true);
        
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        
        gl.glStencilFunc(gl.GL_EQUAL, 0x1, 0x1);
        gl.glStencilOp(gl.GL_KEEP, gl.GL_KEEP, gl.GL_KEEP);
        
        JoglCameraRenderer.activateGL(gl, cameraR);
        escena.drawScene(gl, glu);
        
        gl.glClear(GL.GL_DEPTH_BUFFER_BIT);
        
        gl.glStencilFunc(gl.GL_NOTEQUAL, 0x1, 0x1);
        
        JoglCameraRenderer.activateGL(gl, cameraL);
        escena.drawScene(gl, glu);
    }
    
    /** Not used method, but needed to instanciate GLEventListener */
    public void init(GLDrawable drawable)
    {
        GL gl = drawable.getGL();
        GLU glu = drawable.getGLU();
        
        gl.glEnable(gl.GL_DEPTH_TEST);
        
        gl.glMatrixMode(gl.GL_MODELVIEW);
        gl.glClearColor(0,0,0,0);
        gl.glClearDepth(1.0);
        gl.glDepthFunc(gl.GL_LESS);
        gl.glShadeModel(gl.GL_SMOOTH);
        
        escena.initScene(gl, glu);
        
        gl.glMatrixMode(gl.GL_PROJECTION);
        
        gl.glLoadIdentity();
        
        drawable.getGLU().gluPerspective(45.0f, (float)canvas.getSize().width / (float)canvas.getSize().height, 0.1f, 100.0f);
        
        gl.glMatrixMode(gl.GL_MODELVIEW);
        
    }
    
    
    /** Not used method, but needed to instanciate GLEventListener */
    public void displayChanged(GLDrawable drawable, boolean a, boolean b)
    {
    }
    
    private void prepareStencil(GL gl, int w, int h)
    {
        byte[] interlineado=new byte[w*h];
        
        for(int i=0; i<h; i+=2)
        {
            for(int j=0; j<w; j++)
            {
                interlineado[i*w+j]=(byte)0xffffffff;
            }
        }
        gl.glClear(gl.GL_STENCIL_BUFFER_BIT);
        gl.glDrawPixels(w, h, gl.GL_STENCIL_INDEX, gl.GL_UNSIGNED_BYTE, interlineado);
    }
    
    
    /** Called to indicate the drawing surface has been moved and/or resized */
    public void reshape(GLDrawable drawable,int x,int y,int width,int height)
    {
        GL gl = drawable.getGL();
        gl.glViewport(0, 0, width, height);
        
        prepareStencil(gl, width, height);
        
        cameraL.updateViewportResize(width, height);
        cameraR.updateViewportResize(width, height);
    }
    
    public void actionPerformed(ActionEvent e)
    {
        if(e.getSource()==miSalir)
        {
            System.exit(0);
        }
        
        if(e.getSource()==miConfig)
        {
            frameConfig.setVisible(true);
        }
        
        if(e.getSource()==bAceptar)
        {
            frameConfig.setVisible(false);
        }
        
    }
    
    private class ControlPanel extends JPanel implements ChangeListener
    {
        private JSlider slDEye=new JSlider(-200,200);
        
        public ControlPanel()
        {
            setLayout(new BorderLayout());
            
            JLabel lDEye=new JLabel("Distancia entre ojos");
            
            slDEye.addChangeListener(this);
            slDEye.setValue(0);
            
            JPanel pDist=new JPanel();
            pDist.add(lDEye);
            pDist.add(slDEye);
            
            add(pDist, BorderLayout.SOUTH);
        }

        public void stateChanged(ChangeEvent e)
        {
            if(e.getSource()==slDEye)
            {
                dEye=((float)slDEye.getValue())/1000;
            }
        }
        
    }}
