package vsdk.toolkit.environment.scene;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;

/**
Pair in the column-vector convention. A valid pair maps geometry to its
parent and back without changing affine ray parameters.
*/
public class AffinePlacement implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private Matrix4x4d forward;

    private Matrix4x4d inverse;

    private AffinePlacementStatus status;

    public AffinePlacement() {
        this.forward = new Matrix4x4d();
        this.inverse = new Matrix4x4d();
        this.status = AffinePlacementStatus.Valid;}

    public AffinePlacementStatus assign(Matrix4x4d value, Matrix4x4d inverseValue) {
    forward = value;
    inverse = inverseValue;
    status = validate(forward, inverse);
    return status;
}

    public Matrix4x4d getForward() { return forward; }

    public Matrix4x4d getInverse() { return inverse; }

    public AffinePlacementStatus getStatus() { return status; }

    public static AffinePlacementStatus validate(Matrix4x4d forward, Matrix4x4d inverse) {
    for (int row=0; row<4; ++row) for (int column=0; column<4; ++column) {
        if (!Double.isFinite(forward.get(row,column))) return AffinePlacementStatus.Invalid;
        if (!Double.isFinite(inverse.get(row,column))) return AffinePlacementStatus.Indeterminate;
    }
    if (forward.get(3,0)!=0 || forward.get(3,1)!=0 || forward.get(3,2)!=0 || forward.get(3,3)!=1 ||
        inverse.get(3,0)!=0 || inverse.get(3,1)!=0 || inverse.get(3,2)!=0 || inverse.get(3,3)!=1)
        return AffinePlacementStatus.Invalid;
    final double a=forward.get(0,0), b=forward.get(0,1), c=forward.get(0,2);
    final double d=forward.get(1,0), e=forward.get(1,1), f=forward.get(1,2);
    final double g=forward.get(2,0), h=forward.get(2,1), i=forward.get(2,2);
    final double determinant=a*(e*i-f*h)-b*(d*i-f*g)+c*(d*h-e*g);
    if (!Double.isFinite(determinant)) return AffinePlacementStatus.Indeterminate;
    if (determinant==0) {
        // A visibly zero row or column proves singularity. Otherwise the
        // determinant may have underflowed or suffered cancellation.
        for (int k=0; k<3; ++k) {
            if ((forward.get(k,0)==0 && forward.get(k,1)==0 && forward.get(k,2)==0) ||
                (forward.get(0,k)==0 && forward.get(1,k)==0 && forward.get(2,k)==0))
                return AffinePlacementStatus.Invalid;
        }
        return AffinePlacementStatus.Indeterminate;
    }
    final Matrix4x4d[] products = {forward.multiply(inverse), inverse.multiply(forward)};
    for (int k=0; k<2; ++k) for (int row=0; row<4; ++row) for (int column=0; column<4; ++column) {
        final double actual=products[k].get(row,column), expected=row==column ? 1.0 : 0.0;
        if (!Double.isFinite(actual) || Math.abs(actual-expected)>1e-8)
            return AffinePlacementStatus.Indeterminate;
    }
    return AffinePlacementStatus.Valid;
}
}
