package vsdk.toolkit.media;
import java.io.Serial;

import vsdk.toolkit.common.dataStructures.ArrayListOfDoubles;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;

/**
The Calligraphic3DBuffer class represents a set of 3D line segments, the
input of a vector graphics device before projection, in the same way that
`Calligraphic2DBuffer` represents its already projected 2D output.

It is the generic representation to which the 1-dimensional geometries
(i.e. curves) and the line annotations of a scene (i.e. a reference grid) are
exported to be drawn without a 3D graphics API: the lines are projected by a
camera, and can have their hidden parts removed by the quantitative
invisibility algorithm of `HiddenLineRenderer`, where they are "free lines":
they are hidden by solids, but do not hide anything.

As a media entity it is not a Geometry: it has no interpretation as a
modelled shape (no containment, no intersection), only as lines to be
drawn. Coordinates are those of the space where it was built (i.e. object or
world coordinates).
*/
public class Calligraphic3DBuffer extends MediaEntity {
    @Serial private static final long serialVersionUID = 20261117L;

    /// Values stored per line: x0, y0, z0, x1, y1, z1, R, G, B, width
    private static final int LINE_SIZE = 10;

    private ArrayListOfDoubles lineData;

    public Calligraphic3DBuffer()
    {
        lineData = new ArrayListOfDoubles(LINE_SIZE * 1000);
    }

    /**
    Erases all of internal calligraphy contents
    */
    public void init()
    {
        lineData.clean();
    }

    /**
    Adds a 3D line to the calligraphic buffer.
    */
    public void add3DLine(Vector3Dd p0, Vector3Dd p1) {
        add3DLine(p0.x(), p0.y(), p0.z(), p1.x(), p1.y(), p1.z());
    }

    /**
    Adds a 3D line to the calligraphic buffer.
    */
    public void add3DLine(double x0, double y0, double z0,
                          double x1, double y1, double z1) {
        lineData.add(x0);
        lineData.add(y0);
        lineData.add(z0);
        lineData.add(x1);
        lineData.add(y1);
        lineData.add(z1);
        lineData.add(0);  // R
        lineData.add(0);  // G
        lineData.add(0);  // B
        lineData.add(1.0); // Width
    }

    /**
    @param i index of the line, from 0 to `getNumLines() - 1`
    @return the two end points of the line
    */
    public Vector3Dd[] get3DLine(int i)
    {
        int base = LINE_SIZE * i;
        Vector3Dd outP0 = new Vector3Dd(lineData.get(base),
            lineData.get(base + 1), lineData.get(base + 2));
        Vector3Dd outP1 = new Vector3Dd(lineData.get(base + 3),
            lineData.get(base + 4), lineData.get(base + 5));
        return new Vector3Dd[] { outP0, outP1 };
    }

    public int getNumLines()
    {
        return lineData.size() / LINE_SIZE;
    }
}
