//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - November 5 2006 - Oscar Chavarro: Original base version               =
//===========================================================================

package vsdk.toolkit.media;

import java.util.ArrayList;

import vsdk.toolkit.common.ArrayListOfDoubles;
import vsdk.toolkit.common.Entity;
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.Vector3D;

/**
The Calligraphic2DBuffer class represents a set of elements suitable for a
vector graphics device, like calligraphic CRT, vectorized postscript
and conventional/legacy pen-plotters.

This class is to calligraphic devices like the Image class is to raster
devices.

The nature of this class is structurally 2D, so must not be treated as a
Geometry as doesn't live in 3D space. Nevertheless, could be use as an
argument or modifier for 3D Geometry, in the same way an Image could be
used as a map (i.e. texture or colormap, depthmap, bumpmap, etc).

This class doen' t impose any interpretation on coordinates, but it is
suggested that internal double coordinates be mapped to the range 
<-1, -1, -1> to <1, 1, 1>.
*/
public class Calligraphic2DBuffer extends Entity {
    /// Check the general attribute description in superclass Entity.
    public static final long serialVersionUID = 20060502L;

    private ArrayListOfDoubles lineData;

    public Calligraphic2DBuffer()
    {
        lineData = new ArrayListOfDoubles(80000); // 10000 lines
    }

    /**
    Erases all of internal calligraphy contents
    */
    public void init()
    {
        lineData.clean();
    }

    /**
    Adds a 2D line to the calligraphic buffer. Note that z components in
    Vector coordinates are discarged.
    */
    public void add2DLine(Vector3D p0, Vector3D p1) {
        add2DLine(p0.x, p0.y, p1.x, p1.y);
    }

    /**
    Adds a 2D line to the calligraphic buffer.
    */
    public void add2DLine(double x0, double y0, double x1, double y1) {
        lineData.append(x0);
        lineData.append(y0);
        lineData.append(x1);
        lineData.append(y1);
        lineData.append(0);  // R
        lineData.append(0);  // G
        lineData.append(0);  // B
        lineData.append(1.0); // Width
    }

    public void get2DLine(int i, Vector3D outP0, Vector3D outP1)
    {
        outP0.x = lineData.array[8*i];
        outP0.y = lineData.array[8*i+1];
        outP0.z = 0.0;
        outP1.x = lineData.array[8*i+2];
        outP1.y = lineData.array[8*i+3];
        outP1.z = 0.0;
    }

    public int getNumLines()
    {
        return lineData.size/8;
    }

    public void finalize()
    {
        init();
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
