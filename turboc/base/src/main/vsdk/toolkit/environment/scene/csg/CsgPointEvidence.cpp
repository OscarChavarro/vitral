#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/environment/scene/csg/CsgPointEvidence.h"
CsgPointEvidence::CsgPointEvidence() : equivalence(FALSE), classification(GeometryContainment::Indeterminate), point(), tolerance(0) {}
CsgPointEvidence::CsgPointEvidence(const CsgPointWitness& p, GeometryContainment::Value c,
    const Vector3Dd& q, double t) : path(p), equivalence(FALSE), classification(c), point(q), tolerance(t) {}
CsgPointWitness& CsgPointEvidence::getPath() { return path; }
const CsgPointWitness& CsgPointEvidence::getPath() const { return path; }
GeometryContainment::Value CsgPointEvidence::getClassification() const { return classification; }
const Vector3Dd& CsgPointEvidence::getPoint() const { return point; }
double CsgPointEvidence::getTolerance() const { return tolerance; }

CsgPointEvidence::CsgPointEvidence(const CsgPointWitness& source, const CsgPointWitness& equivalent,
    const Vector3Dd& q, double t) : path(source), equivalentPath(equivalent), equivalence(TRUE),
    classification(GeometryContainment::Indeterminate), point(q), tolerance(t) {}
BOOLEAN CsgPointEvidence::isEquivalence() const { return equivalence; }
const CsgPointWitness& CsgPointEvidence::getEquivalentPath() const { return equivalentPath; }
BOOLEAN CsgPointEvidence::prepend(const CsgOperandView* operand)
{ return path.prepend(operand) && (!equivalence || equivalentPath.prepend(operand)); }
void CsgPointEvidence::reverseOrientation()
{ path.reverseOrientation(); if (equivalence) equivalentPath.reverseOrientation(); }
