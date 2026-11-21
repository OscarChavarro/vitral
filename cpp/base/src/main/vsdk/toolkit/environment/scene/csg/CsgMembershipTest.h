#ifndef __CSG_MEMBERSHIP_TEST__
#define __CSG_MEMBERSHIP_TEST__
// How the POV crossing-membership filter decides whether an event point lies
// in a sibling operand. SIGN, the default, is the inside test of POV-Ray 1.0:
// the point is mapped into each primitive's own coordinates and the sign of
// its implicit function there decides (zero counts as inside), folded with the
// CSG rules; it costs one evaluation per primitive. CERTIFIED_BALL classifies
// the whole tolerance ball around the point with outward-rounded interval
// arithmetic: it proves each decision, at several times the cost, and on
// processors that complete denormal arithmetic in software (Alpha with
// -mieee) far more.
enum class CsgMembershipTest { SIGN, CERTIFIED_BALL };
#endif
