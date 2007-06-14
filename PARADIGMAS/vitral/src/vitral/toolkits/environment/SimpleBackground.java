//===========================================================================
package vitral.toolkits.environment;

import vitral.toolkits.common.ColorRgb;
import vitral.toolkits.common.Vector3D;

public class SimpleBackground extends Background {
    ColorRgb _color;

    public SimpleBackground() {
        super();

        _color = new ColorRgb();
        _color.r = 0;
        _color.g = 0;
        _color.b = 0;
    }

    public ColorRgb color_en_direccion(Vector3D d)
    {
        return _color;
    }

    public void set_color(float r, float g, float b)
    {
        _color.r = r;
        _color.g = g;
        _color.b = b;
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
