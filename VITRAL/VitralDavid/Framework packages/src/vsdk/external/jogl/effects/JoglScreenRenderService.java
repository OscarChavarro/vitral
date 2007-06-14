package vsdk.external.jogl.effects;

import java.util.*;

import vsdk.framework.render.*;

import vsdk.toolkit.media.RGBAImage;
import vsdk.toolkit.environment.*;
import vsdk.toolkit.environment.geometry.*;
import vsdk.toolkit.render.jogl.*;
import vsdk.toolkit.common.*;

import javax.media.opengl.GL;
import javax.media.opengl.glu.GLU;

public class JoglScreenRenderService extends JoglRenderService
{
	private GL gl;
	private GLU glu;
	private QualitySelection qs;
	private int lightNumber;
	
	public JoglScreenRenderService(GL gl, GLU glu, QualitySelection qs)
	{
		this.gl=gl;
		this.glu=glu;
		this.qs=qs;
	}
	
	public synchronized void setQualitySelection(QualitySelection qs)
	{
		this.qs=qs;
	}

    public void initService(GL gl, GLU glu)
    {
        gl.glEnable(gl.GL_BLEND);
        gl.glBlendFunc(gl.GL_SRC_ALPHA, gl.GL_ONE_MINUS_SRC_ALPHA);
        
        gl.glEnable(gl.GL_LIGHTING);
        gl.glEnable(gl.GL_LIGHT0);
        
        gl.glClearColor(0.0f,0.0f,0.0f,1.0f);
        
        gl.glClearDepth(1.0);
        
        gl.glDepthFunc(gl.GL_LESS);
        
        gl.glEnable(gl.GL_DEPTH_TEST);
        
        gl.glShadeModel(gl.GL_SMOOTH);
    }
    
	public synchronized void render()
	{
        gl.glClear(gl.GL_COLOR_BUFFER_BIT | gl.GL_DEPTH_BUFFER_BIT);
		
        JoglCameraRenderer.activateGL(gl, camera);
        
		for(Object o : data.getObjects())
		{
			if(o instanceof Light)
			{
				Light lAux=(Light)o;
				lAux.setId(lightNumber);
				lightNumber++;
				JoglLightRenderer.activate(gl, lAux);
			}
		}
		lightNumber=0;
		
		for(Object o : data.getObjects())
		{
			if(o instanceof RayableObject)
			{
				JoglRayableObjectRenderer.draw(gl, (RayableObject)o, qs);
			}
			else if(o instanceof MeshGroup)
			{
				JoglMeshGroupRenderer.draw(gl, (MeshGroup)o, qs);
			}
			else if(o instanceof Mesh)
			{
				JoglMeshRenderer.draw(gl, (Mesh)o, qs, false);
			}
			else if(o instanceof Arrow)
			{
				JoglArrowRenderer.draw(gl, (Arrow)o, camera, qs);
			}
			else if(o instanceof Box)
			{
				JoglBoxRenderer.draw(gl, (Box)o, camera, qs);
			}
			else if(o instanceof Cone)
			{
				JoglConeRenderer.draw(gl, (Cone)o, camera, qs);
			}
			else if(o instanceof Sphere)
			{
				JoglSphereRenderer.draw(gl, (Sphere)o, camera, qs);
			}
			else if(o instanceof QualitySelection)
			{
				qs=(QualitySelection)o;
			}
		}
	}

	public RGBAImage getImage()
	{
		return null;//not yet implemented
	}
}
