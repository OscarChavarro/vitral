package vsdk.toolkit.environment.geometry.surface;
import java.io.Serial;

import vsdk.toolkit.common.Ray;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.GeometryIntersectionInformation;

public abstract class Curve extends Geometry {
    @SuppressWarnings("FieldNameHidesFieldInSuperclass")
    @Serial private static final long serialVersionUID = 20150218L;
    
    /**
    This method is provided to ease the integration with generic operation
    Geometry.doIntersection.  A default behavior of not responding the test
    is provided here for 1-dimensional forms. Note that a Loft creation
    between current curve and a circle curve for emulating a tube like
    structure, gives as a result a 2-dimensional Surface, from which a
    Loft.doIntersection operation will give similar results to that
    expected from this 1-dimensional case. However, as real mathematical
    1-dimensional objects are infinitively thin, a doIntersection operation
    will always fail as the operation is regularized for constructive
    solid modelling compatible interpretation.
    @param r
    @return always if false
    */
    @Override
    public Ray doIntersection(Ray r)
    {
        return null;
    }

    /**
    Check the general interface contract in superclass method
    Geometry.doExtraInformation.
    @param inRay
    @param intT
    @param outData
    */
    @Override
    public void
    doExtraInformation(Ray inRay, double intT, 
                                      GeometryIntersectionInformation outData)
    {
        
    }
}
