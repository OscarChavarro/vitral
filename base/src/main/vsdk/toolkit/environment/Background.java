package vsdk.toolkit.environment;
import java.io.Serial;

import vsdk.toolkit.common.Entity;
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Vector3D;

public abstract class Background extends Entity
{
    @SuppressWarnings("FieldNameHidesFieldInSuperclass")
    @Serial private static final long serialVersionUID = 20150218L;
    
    public Background() {

    }
    public abstract ColorRgb colorInDireccion(Vector3D d);
}
