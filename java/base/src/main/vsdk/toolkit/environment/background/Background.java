package vsdk.toolkit.environment.background;
import java.io.Serial;

import vsdk.toolkit.common.Entity;
import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;

public abstract class Background extends Entity
{
    @SuppressWarnings("FieldNameHidesFieldInSuperclass")
    @Serial private static final long serialVersionUID = 20150218L;
    
    public Background() {

    }
    public abstract ColorRgb colorInDireccion(Vector3Dd d);
}
