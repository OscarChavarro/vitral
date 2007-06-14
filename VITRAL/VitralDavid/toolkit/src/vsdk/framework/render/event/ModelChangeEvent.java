package vsdk.framework.render.event;

import vsdk.framework.render.*;
import java.util.*;

public class ModelChangeEvent
{
    private Renderizable model;
    private Properties info;
    
    public ModelChangeEvent(Renderizable model, Properties info)
    {
        this.model=model;
        this.info=info;
    }
    
    public Renderizable getModel()
    {
        return model;
    }
    
    public Properties getInfo()
    {
        return info;
    }
}
