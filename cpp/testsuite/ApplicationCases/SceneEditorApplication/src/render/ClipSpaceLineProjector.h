#ifndef __CLIP_SPACE_LINE_PROJECTOR__
#define __CLIP_SPACE_LINE_PROJECTOR__

#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"

class Calligraphic2DBuffer;
class Camera;

/**
Projects 3D segments and points to the normalized device coordinates of the
view of a camera, clipping them against its view volume in homogeneous clip
space (before the perspective division, so segments crossing the plane of
the eye are handled), as the geometry stage of a 3D graphics API does.
*/
class ClipSpaceLineProjector {
private:
    Matrix4x4d projection;

public:
    /**
    @param camera camera whose view is projected; its viewport size must be
    already updated (it gives the aspect ratio)
    */
    explicit ClipSpaceLineProjector(const Camera* camera);

    /**
    Projects a segment given in world coordinates.
    @param out buffer receiving the projected segment, if any part of it is
    inside the view volume
    @return true if a segment was added
    */
    bool addSegment(Calligraphic2DBuffer* out, const Vector3Dd& p0,
                    const Vector3Dd& p1) const;

    /**
    Projects a segment given in the coordinates of an object.
    @param transform transformation from object to world coordinates
    @return true if a segment was added
    */
    bool addSegment(Calligraphic2DBuffer* out, const Matrix4x4d& transform,
                    const Vector3Dd& p0, const Vector3Dd& p1) const;

    /**
    Projects a point, given in world coordinates, as a degenerate segment.
    @return true if the point was added
    */
    bool addPoint(Calligraphic2DBuffer* out, const Vector3Dd& p) const;
};

#endif
