package vsdk.toolkit.environment.scene.csg;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;

/** Borrowed identity and owned child-to-parent and inverse snapshots. */
public class CsgRayPlacement {
    public CsgOperandView operand;
    public Matrix4x4d childToParent;
    public Matrix4x4d parentToChild;

    public CsgRayPlacement() { operand = null; childToParent = new Matrix4x4d(); parentToChild = new Matrix4x4d(); }

    public CsgRayPlacement(CsgOperandView value, Matrix4x4d forward, Matrix4x4d inverse)
    { operand = value; childToParent = forward; parentToChild = inverse; }
}
