#include <cstring>

#include "models/CsgSampleNames.h"

static const char* const NAMES[] = {
    "MANT1986_2",
    "STACKED_BLOCKS",
    "MOON_BLOCK",
    "CROSS_PAIR",
    "HOLLOW_BRICK",
    "MANT1988_6_13",
    "MANT1988_15_1",
    "MANT1988_3",
    "MANT1988_15_2_HOLED",
    "MANT1988_15_2_LIMIT_DIFFERENCE",
    "MANT1988_15_2_OPEN_DIFFERENCE",
    "KURLANDER_BOWL_SINGLE_MOTIF"
};

static const int IDS[] = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12 };

static const int VALUES_LENGTH = (int)(sizeof(NAMES) / sizeof(NAMES[0]));

const char* CsgSampleNamesUtil::name(CsgSampleNames value)
{
    return NAMES[(int)value];
}

CsgSampleNames CsgSampleNamesUtil::nextCircular(CsgSampleNames value)
{
    int nextIndex = ((int)value + 1) % VALUES_LENGTH;
    return (CsgSampleNames)nextIndex;
}

const char* CsgSampleNamesUtil::getLabel(CsgSampleNames value)
{
    return name(value);
}

CsgOperationNames CsgSampleNamesUtil::getPreferredOperation(
    CsgSampleNames value, CsgOperationNames currentOperation)
{
    if ( value == CsgSampleNames::MANT1988_15_2_LIMIT_DIFFERENCE ||
         value == CsgSampleNames::MANT1988_15_2_OPEN_DIFFERENCE ||
         value == CsgSampleNames::KURLANDER_BOWL_SINGLE_MOTIF /*|| this == KURLANDER_BOWL_ALL_MOTIFS*/ ) {
        return CsgOperationNames::DIFFERENCE_A_MINUS_B;
    }
    return currentOperation;
}

int CsgSampleNamesUtil::getDisplayIndex(CsgSampleNames value)
{
    return IDS[(int)value];
}

int CsgSampleNamesUtil::getTotalSamples()
{
    return VALUES_LENGTH;
}

CsgSampleNames CsgSampleNamesUtil::fromOrdinal(int ordinal)
{
    return (CsgSampleNames)ordinal;
}

bool CsgSampleNamesUtil::valueOf(const char* name, CsgSampleNames* out)
{
    int i;
    for ( i = 0; i < VALUES_LENGTH; i++ ) {
        if ( std::strcmp(NAMES[i], name) == 0 ) {
            *out = (CsgSampleNames)i;
            return true;
        }
    }
    return false;
}
