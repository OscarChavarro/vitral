package vsdk.framework.render;

import java.util.*;
import vsdk.framework.render.event.*;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.gui.CameraController;
import vsdk.toolkit.gui.CameraControllerGravZero;
import vsdk.toolkit.media.*;

public abstract class RenderService
{
    protected Renderizable data;
    private Properties props;
    private LinkedList<ModelChangeStrategy> modelChangeStrategies;
    
    protected Camera camera=new Camera();
    protected CameraController camControl=new CameraControllerGravZero(camera);
    
    public abstract void render();
    public abstract RGBAImage getImage();
    
    public void setData(Renderizable r)
    {
        data=r;
    }
    
    public Properties getProperties()
    {
        return props;
    }
    
    public String getProperty(String name)
    {
        return props.getProperty(name);
    }
    
    public void setProperty(String name, String value)
    {
        props.put(name, value);
    }
    
    public void addModelChangeStrategy(ModelChangeStrategy mcs)
    {
        modelChangeStrategies.add(mcs);
    }
    
    public void removeModelChangeStrategy(ModelChangeStrategy mcs)
    {
        modelChangeStrategies.remove(mcs);
    }
    
    public void removeAllModelChangeStrategies()
    {
        modelChangeStrategies.removeAll(modelChangeStrategies);
    }
    
    public Collection<ModelChangeStrategy> getModelChangeStrategies()
    {
        return modelChangeStrategies;
    }
    
    public void fireModelChangeEvent(ModelChangeEvent e)
    {
        for(ModelChangeStrategy mcs: modelChangeStrategies)
        {
            mcs.modelStateChanged(e);
        }
    }
    
    public Camera getCamera()
    {
        return camera;
    }
    
    public CameraController getCameraController()
    {
        return camControl;
    }
    
    public void setCamera(Camera c)
    {
        camera=c;
    }
    
    public void setCameraController(CameraController c)
    {
        camControl=c;
    }
    
    public void setViewportSize(int w, int h)
    {
        camera.updateViewportResize(w, h);
    }
    
}
