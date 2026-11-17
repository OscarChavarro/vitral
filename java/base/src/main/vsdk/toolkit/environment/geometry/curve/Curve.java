package vsdk.toolkit.environment.geometry.curve;
import java.io.Serial;

import vsdk.toolkit.environment.geometry.element.Ray;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.element.RayHit;
import vsdk.toolkit.media.Calligraphic3DBuffer;

public abstract class Curve extends Geometry {
    @SuppressWarnings("FieldNameHidesFieldInSuperclass")
    @Serial private static final long serialVersionUID = 20150218L;
    
    /**
    This method is provided to ease the integration with generic operation
    Geometry.doIntersectionFirstHit.  A default behavior of not responding the test
    is provided here for 1-dimensional forms. Note that a Loft creation
    between current curve and a circle curve for emulating a tube like
    structure, gives as a result a 2-dimensional Surface, from which a
    Loft.doIntersectionFirstHit operation will give similar results to that
    expected from this 1-dimensional case. However, as real mathematical
    1-dimensional objects are infinitively thin, a doIntersectionFirstHit operation
    will always fail as the operation is regularized for constructive
    solid modelling compatible interpretation.
    @param r
    @return always if false
    */
    public Ray doIntersectionFirstHit(Ray r)
    {
        return null;
    }

    @Override
    public boolean doIntersectionFirstHit(Ray inRay, RayHit outHit)
    {
        Ray hit = doIntersectionFirstHit(inRay);
        if ( hit == null ) {
            return false;
        }
        if ( outHit != null ) {
            outHit.setRay(hit);
            doExtraInformation(hit, hit.getT(), outHit);
        }
        return true;
    }

    /**
    Check the general interface contract in superclass method
    Geometry.doExtraInformation.
    @param inRay
    @param intT
    @param outData
    */
    public void
    doExtraInformation(Ray inRay, double intT, 
                                      RayHit outData)
    {
        
    }

    /**
    Exports a line approximation of current curve, as the 3D segments of a
    polyline per curve segment, in the coordinates of the curve. It is the
    generic representation used to draw curves without a 3D graphics API,
    and to remove their hidden parts (see `HiddenLineRenderer`).
    @return a new buffer with the segments approximating the curve
    */
    public abstract Calligraphic3DBuffer exportToCalligraphic3DBuffer();
}
