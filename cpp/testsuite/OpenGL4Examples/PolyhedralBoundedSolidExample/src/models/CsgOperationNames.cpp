#include "models/CsgOperationNames.h"

static const char* const NAMES[] = {
    "UNION", "INTERSECTION", "DIFFERENCE_A_MINUS_B", "DIFFERENCE_B_MINUS_A"
};

static const char* const LABELS[] = {
    "UNION", "INTERSECTION", "DIFFERENCE A-B", "DIFFERENCE B-A"
};

static const int VALUES_LENGTH = 4;

const char* CsgOperationNamesUtil::name(CsgOperationNames value)
{
    return NAMES[(int)value];
}

CsgOperationNames CsgOperationNamesUtil::nextCircular(CsgOperationNames value)
{
    int nextIndex = ((int)value + 1) % VALUES_LENGTH;
    return (CsgOperationNames)nextIndex;
}

const char* CsgOperationNamesUtil::getLabel(CsgOperationNames value)
{
    return LABELS[(int)value];
}
