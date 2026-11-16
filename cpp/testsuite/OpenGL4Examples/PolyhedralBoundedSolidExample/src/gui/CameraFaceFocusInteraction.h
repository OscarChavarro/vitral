#ifndef __POLYHEDRAL_EXAMPLE_CAMERA_FACE_FOCUS_INTERACTION__
#define __POLYHEDRAL_EXAMPLE_CAMERA_FACE_FOCUS_INTERACTION__

#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"

class Camera;
class DebuggerModel;
class _PolyhedralBoundedSolidFace;
class _PolyhedralBoundedSolidVertex;

/// C++ port of Java's `gui.CameraFaceFocusInteraction`
class CameraFaceFocusInteraction {
public:
    bool focusSelectedFace(DebuggerModel& model);

private:
    static java::ArrayList<_PolyhedralBoundedSolidVertex*> collectFaceVertices(
        _PolyhedralBoundedSolidFace* face);
    static Vector3Dd computeCentroid(
        const java::ArrayList<_PolyhedralBoundedSolidVertex*>& vertices);
    static Vector3Dd chooseFacingFront(const Vector3Dd& normal,
        const Vector3Dd& center,
        const Vector3Dd& cameraPosition);
    static Vector3Dd chooseUpVector(const Vector3Dd& upHint,
        const Vector3Dd& front);
    static double computeFramingDistance(Camera* camera,
        const Vector3Dd& center,
        const Vector3Dd& front,
        const Vector3Dd& left,
        const Vector3Dd& up,
        const java::ArrayList<_PolyhedralBoundedSolidVertex*>& vertices);
};

#endif
