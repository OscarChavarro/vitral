#ifndef __POLYHEDRAL_EXAMPLE_JOGL4_HUD_OPERANDS_RENDERER__
#define __POLYHEDRAL_EXAMPLE_JOGL4_HUD_OPERANDS_RENDERER__

#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/material/SimpleMaterial.h"

class Camera;
class DebuggerModel;
class PolyhedralBoundedSolid;

/**
C++ port of Java's `render.Jogl4HudOperandsRenderer`: the CSG operand
insets drawn under the HUD. As in Java, the program does not use it (the
insets are drawn by `Jogl4DebuggerRenderer`).
*/
class Jogl4HudOperandsRenderer {
public:
    explicit Jogl4HudOperandsRenderer(DebuggerModel* model);

    void draw(int viewportWidth, int viewportHeight);

private:
    DebuggerModel* model;
    SimpleMaterial csgOperandMaterialA;
    SimpleMaterial csgOperandMaterialB;

    static SimpleMaterial createInsetMaterial(double r, double g, double b);
    static Vector3Dd solidCenter(PolyhedralBoundedSolid* solid);
    static double solidMaxExtent(PolyhedralBoundedSolid* solid);
    static Vector3Dd cameraRelativeAnchor(Camera* camera, double ndcX,
        double ndcY, double depth);
    void drawInsetSolid(PolyhedralBoundedSolid* solid,
        const SimpleMaterial& material, const Vector3Dd& anchorPoint,
        double mainSolidExtent);
};

#endif
