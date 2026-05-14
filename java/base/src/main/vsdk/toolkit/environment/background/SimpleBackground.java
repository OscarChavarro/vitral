package vsdk.toolkit.environment.background;
import java.io.Serial;

import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;

public class SimpleBackground extends Background {
    @Serial private static final long serialVersionUID = 20060502L;

    private ColorRgb _color;

    public SimpleBackground() {
        super();

        _color = new ColorRgb(0, 0, 0);
    }

    @Override
    public ColorRgb colorInDireccion(Vector3Dd d)
    {
        return new ColorRgb(_color);
    }

    public void setColor(double r, double g, double b)
    {
        _color = new ColorRgb(r, g, b);
    }
}
