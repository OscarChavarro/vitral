#ifndef __CSG_BODY_EVALUATOR__
#define __CSG_BODY_EVALUATOR__

// Ray evaluator of a CsgBody: the regularized [ROTH1982] classification
// (CsgRothRayClassifier) or the POV-Ray 1.0 crossing membership heuristic
// (CsgCrossingMembershipFilter), which keeps union internal surfaces.
enum class CsgBodyEvaluator { ROTH, CROSSING_MEMBERSHIP };
#endif
