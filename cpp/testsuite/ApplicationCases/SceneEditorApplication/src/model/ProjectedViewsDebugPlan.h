//= References:                                                             =
//= [FUNK2003], Funkhouser, Thomas.  Min, Patrick. Kazhdan, Michael. Chen,  =
//=     Joyce. Halderman, Alex. Dobkin, David. Jacobs, David. "A Search     =
//=     Engine for 3D Models", ACM Transactions on Graphics, Vol 22. No1.   =
//=     January 2003. Pp. 83-105                                            =

#ifndef __PROJECTED_VIEWS_DEBUG_PLAN__
#define __PROJECTED_VIEWS_DEBUG_PLAN__

#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"

class Camera;

/**
Placement of the 13 boxes (3 axis aligned faces and 10 diagonal ones) used to
present the projected views of a body for debugging, and of the cameras that
take those views. Views are numbered from 1 to `VIEW_COUNT`.
*/
class ProjectedViewsDebugPlan {
public:
    static const int VIEW_COUNT = 13;

    /**
    Position, scale and orientation of the box presenting one projected view.
    */
    class ViewPlacement {
    private:
        Vector3Dd position;
        Vector3Dd scale;
        Matrix4x4d rotation;

    public:
        ViewPlacement() {}
        ViewPlacement(const Vector3Dd& position, const Vector3Dd& scale,
                      const Matrix4x4d& rotation)
            : position(position), scale(scale), rotation(rotation) {}

        const Vector3Dd& getPosition() const { return position; }
        const Vector3Dd& getScale() const { return scale; }
        const Matrix4x4d& getRotation() const { return rotation; }
    };

private:
    ProjectedViewsDebugPlan() {}

    static Matrix4x4d rotation(double degrees, double x, double y, double z);
    static Matrix4x4d composed(const Matrix4x4d& first,
                               const Matrix4x4d& second);
    static Vector3Dd diagonalPosition(double x, double y, double z);

public:
    /**
    @param view number of the view, from 1 to `VIEW_COUNT`
    @param outPlacement the placement of the box for that view
    @return false if the number is out of range (the Java version returns
    null)
    */
    static bool getPlacement(int view, ViewPlacement* outPlacement);

    /**
    Creates the camera that takes one projected view of a body normalized
    inside the unit cube, as decribed in [FUNK2003].5, and figure
    [FUNK2003].8 (orthogonal projection, 10 units away from the origin).
    Views are:
      - 1   Front side view (from -Y axis)
      - 2   Lateral side view (from -X axis)
      - 3   Top side view (from -Z axis)
      - 4   Corner view from -X -Y Z direction
      - 5   Corner view from  X -Y Z direction
      - 6   Corner view from  X  Y Z direction
      - 7   Corner view from -X  Y Z direction
      - 8   Tilt view edge +Y on plane -Z
      - 9   Tilt view edge -X on plane -Z
      - 10  Tilt view edge -Y on plane -Z
      - 11  Tilt view edge +X on plane -Z
      - 12  Tilt view edge +X on plane -Y
      - 13  Tilt view edge +X on plane Y
    @param view number of the view, from 1 to `VIEW_COUNT`
    @return the camera for that view (at the origin, if the number is out of
    range), owned by the caller
    */
    static Camera* createCamera(int view);
};

#endif
