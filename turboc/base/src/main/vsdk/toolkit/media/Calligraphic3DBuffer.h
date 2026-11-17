#ifndef __CALLIGRAPHIC_3_D_BUFFER__
#define __CALLIGRAPHIC_3_D_BUFFER__

#include "java/util/ArrayList.h"
#include "vsdk/toolkit/media/MediaEntity.h"
class Vector3Dd;

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
class Calligraphic3DBuffer : public MediaEntity {
private:
    /// Values stored per line: x0, y0, z0, x1, y1, z1, R, G, B, width
    enum { LINE_SIZE = 10 };

    ArrayList<double> lineData;

public:
    static const Class CLASS;
    virtual const Class *getClass() const;

    Calligraphic3DBuffer();
    virtual ~Calligraphic3DBuffer() {}

    /**
    Erases all of internal calligraphy contents
    */
    void init();

    /**
    Adds a 3D line to the calligraphic buffer.
    */
    void add3DLine(const Vector3Dd& p0, const Vector3Dd& p1);

    /**
    Adds a 3D line to the calligraphic buffer.
    */
    void add3DLine(double x0, double y0, double z0,
                   double x1, double y1, double z1);

    /**
    @param i index of the line, from 0 to `getNumLines() - 1`
    @param outEnds receives the two end points of the line
    */
    void get3DLine(int i, Vector3Dd outEnds[2]) const;

    int getNumLines() const;
};

#endif
