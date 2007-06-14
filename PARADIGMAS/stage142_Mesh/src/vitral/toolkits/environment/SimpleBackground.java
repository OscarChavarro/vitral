//===========================================================================
package vitral.toolkits.environment;

import vitral.toolkits.common.ColorRgb;
import vitral.toolkits.common.Vector3D;

public class SimpleBackground extends Background 
{
    ColorRgb _color;

    public SimpleBackground() 
    {
    super();
        _color = new ColorRgb();
    }

    public ColorRgb color_en_direccion(Vector3D d)
    {
        return _color;
    }

    public void set_color(ColorRgb c)
    {
        _color=new ColorRgb(c);
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
