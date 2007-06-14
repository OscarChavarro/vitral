package vsdk.framework.render.util;

import java.util.*;

import vsdk.framework.render.Renderizable;

public class TrivialRenderizable implements Renderizable
{
    
    ArrayList<Object> objects=new ArrayList<Object>(); 
    
    public Properties getProperties()
    {
        return null;
    }

    public Collection<Object> getObjects()
    {
        return objects;
    }

    public void addObject(Object o)
    {
        objects.add(o);
    }
}
