package vsdk.external.jogl;

import java.awt.Dimension;

import javax.media.opengl.*;
import javax.media.opengl.glu.*;

import vsdk.framework.presentation.panels.PresentationPanel;
import vsdk.framework.render.RenderService;

import vsdk.toolkit.common.*;
import vsdk.toolkit.gui.adapters.ControllerAdapter;

import vsdk.external.jogl.effects.*;

/**
 * The class JoglMeshRenderizableRenderPanel represents a panel that renders a MeshRenderizable
 * in it using JOGL. 
 * */
public class JoglPresentationPanel extends GLCanvas implements PresentationPanel, GLEventListener, Runnable
{
    private JoglRenderService renderService;
    private QualitySelection qs;
    private GLU glu=new GLU();
    
    private ControllerAdapter adapter;

    public JoglPresentationPanel()
    {
        super();
        this.qs=new QualitySelection();
        renderService=new JoglScreenRenderService(getGL(), glu, qs);
        renderService.setViewportSize(800,600);
        this.setPreferredSize(new Dimension(800,600));
        addGLEventListener(this);
        
        adapter=new ControllerAdapter(renderService.getCameraController());
        adapter.register(this);
        
        Thread t=new Thread(this);
        t.start();
    }
    
    public JoglPresentationPanel(QualitySelection qs)
    {
        super();
        renderService=new JoglScreenRenderService(getGL(), new GLU(), qs);
    }
    
    public void init(GLAutoDrawable draw)
    {
        renderService.initService(draw.getGL(), glu);
    }

    public void display(GLAutoDrawable draw)
    {
        renderService.render();
    }

    public void reshape(GLAutoDrawable draw, int x,int y,int width,int height)
    {
        GL gl = draw.getGL();
        gl.glViewport(0, 0, width, height);
        renderService.setViewportSize(width, height);
    }

    public void displayChanged(GLAutoDrawable draw, boolean arg1, boolean arg2)
    {
        display(draw);
    }

    public void setQualitySelection(QualitySelection qs)
    {
        this.qs=qs;
    }

    public QualitySelection getQualitySelection()
    {
        return qs;
    }

    public void displayData()
    {
        renderService.render();
    }
    
    public void setDisplayMode(String dispMode)
    {
        
    }

    public String[] getDisplayModes()
    {
        String[] ret={"normal"};
        return ret;
    }

    public RenderService getRenderService()
    {
        return renderService;
    }
    
    public void run()
    {
        while(true)
        {
            swapBuffers();
            repaint();
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
