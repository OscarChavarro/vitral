#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/environment/geometry/volume/AffineEllipsoidDistance.h"
#include "vsdk/toolkit/environment/geometry/volume/OrthogonalEllipsoidDistance.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "java/lang/Double.h"
#include "java/lang/Math.h"

GeometryDistanceStatus::Value AffineEllipsoidDistance::query(
    const Vector3Dd& point, double radius, const Matrix4x4d& forward,
    double& distance, double& estimatedError)
{
    Vector3Dd witness;
    return queryWithWitness(point,radius,forward,distance,estimatedError,witness);
}

GeometryDistanceStatus::Value AffineEllipsoidDistance::queryWithWitness(
    const Vector3Dd& point, double radius, const Matrix4x4d& forward,
    double& distance, double& estimatedError, Vector3Dd& witness)
{ int i; int j;
    double candidate=0,guard=0;
    Vector3Dd candidateWitness;
    const GeometryDistanceStatus::Value direct=OrthogonalEllipsoidDistance::queryWithWitness(
        point,radius,forward,candidate,guard,candidateWitness);
    if (direct==GeometryDistanceStatus::Valid) {
        distance=candidate; estimatedError=guard; witness=candidateWitness; return direct;
    }
    if (direct!=GeometryDistanceStatus::Unsupported) return direct;
    // A transformed sphere has principal axes given by the eigenvectors of
    // A A^T. Scale A before forming the product to reduce overflow risk.
    double scale=0;
    for (i =0;i<3;++i) for (j =0;j<3;++j)
        if (Math::abs(forward.get(i,j))>scale) scale=Math::abs(forward.get(i,j));
    if (!(scale>0) || !Double::isFinite(scale) || !Double::isFinite(scale*radius))
        return GeometryDistanceStatus::Indeterminate;
    double matrix[3][3]={{0,0,0},{0,0,0},{0,0,0}};
    double vectors[3][3]={{1,0,0},{0,1,0},{0,0,1}};
    for (i =0;i<3;++i) for (j =0;j<3;++j)
        for (int k=0;k<3;++k) matrix[i][j]+=(forward.get(i,k)/scale)*(forward.get(j,k)/scale);
    BOOLEAN converged=FALSE;
    for (int iteration=0;iteration<80;++iteration) {
        int p=0,q=1;
        if (Math::abs(matrix[0][2])>Math::abs(matrix[p][q])) { p=0; q=2; }
        if (Math::abs(matrix[1][2])>Math::abs(matrix[p][q])) { p=1; q=2; }
        const double off=Math::abs(matrix[p][q]);
        const double diagonal=Math::abs(matrix[0][0])+Math::abs(matrix[1][1])+Math::abs(matrix[2][2]);
        if (off<=1e-14*diagonal) { converged=TRUE; break; }
        const double tau=(matrix[q][q]-matrix[p][p])/(2*matrix[p][q]);
        if (!Double::isFinite(tau)) return GeometryDistanceStatus::Indeterminate;
        const double sign=tau<0 ? -1.0 : 1.0;
        const double tangent=sign/(Math::abs(tau)+Math::sqrt(1+tau*tau));
        const double cosine=1/Math::sqrt(1+tangent*tangent), sine=tangent*cosine;
        if (!Double::isFinite(cosine) || !Double::isFinite(sine)) return GeometryDistanceStatus::Indeterminate;
        const double app=matrix[p][p], aqq=matrix[q][q], apq=matrix[p][q];
        matrix[p][p]=app-tangent*apq;
        matrix[q][q]=aqq+tangent*apq;
        matrix[p][q]=matrix[q][p]=0;
        for (int k=0;k<3;++k) {
            if (k!=p && k!=q) {
                const double kp=matrix[k][p],kq=matrix[k][q];
                matrix[k][p]=matrix[p][k]=cosine*kp-sine*kq;
                matrix[k][q]=matrix[q][k]=sine*kp+cosine*kq;
            }
            const double vp=vectors[k][p],vq=vectors[k][q];
            vectors[k][p]=cosine*vp-sine*vq;
            vectors[k][q]=sine*vp+cosine*vq;
        }
    }
    if (!converged) return GeometryDistanceStatus::Indeterminate;
    // Reject a decomposition whose reconstructed covariance has drifted
    // noticeably from the original. This residual check is not a formal
    // floating-point error bound.
    for (i =0;i<3;++i) for (j =0;j<3;++j) {
        double original=0,reconstructed=0;
        for (int k=0;k<3;++k) {
            original+=(forward.get(i,k)/scale)*(forward.get(j,k)/scale);
            reconstructed+=vectors[i][k]*matrix[k][k]*vectors[j][k];
        }
        if (!Double::isFinite(original) || !Double::isFinite(reconstructed) ||
            Math::abs(original-reconstructed)>1e-11*(1+Math::abs(original)))
            return GeometryDistanceStatus::Indeterminate;
    }
    Matrix4x4d principal=Matrix4x4d::identityMatrix();
    for (i =0;i<3;++i) {
        if (!(matrix[i][i]>1e-12) || !Double::isFinite(matrix[i][i])) return GeometryDistanceStatus::Indeterminate;
        const double axis=scale*radius*Math::sqrt(matrix[i][i]);
        if (!Double::isFinite(axis) || !(axis>0)) return GeometryDistanceStatus::Indeterminate;
        for (int j=0;j<3;++j) principal=principal.withVal(j,i,vectors[j][i]*axis);
        principal=principal.withVal(i,3,forward.get(i,3));
    }
    const GeometryDistanceStatus::Value result=OrthogonalEllipsoidDistance::queryWithWitness(
        point,1,principal,candidate,guard,candidateWitness);
    if (result==GeometryDistanceStatus::Unsupported) return GeometryDistanceStatus::Indeterminate;
    if (result!=GeometryDistanceStatus::Valid) return result;
    const double totalGuard=guard+1e-9*(1+scale*radius);
    if (!Double::isFinite(totalGuard)) return GeometryDistanceStatus::Indeterminate;
    distance=candidate;
    estimatedError=totalGuard;
    witness=candidateWitness;
    return GeometryDistanceStatus::Valid;
}
