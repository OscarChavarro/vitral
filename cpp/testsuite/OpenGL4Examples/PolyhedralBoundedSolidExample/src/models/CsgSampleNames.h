#ifndef __POLYHEDRAL_EXAMPLE_CSG_SAMPLE_NAMES__
#define __POLYHEDRAL_EXAMPLE_CSG_SAMPLE_NAMES__

#include "models/CsgOperationNames.h"

/**
C++ port of Java enum `models.CsgSampleNames`: the enum values are the
ordinals, and the Java identifiers (1 to 12) are `getDisplayIndex`.
*/
enum class CsgSampleNames {
    MANT1986_2,
    STACKED_BLOCKS,
    MOON_BLOCK,
    CROSS_PAIR,
    HOLLOW_BRICK,
    MANT1988_6_13,
    MANT1988_15_1,
    MANT1988_3,
    MANT1988_15_2_HOLED,
    MANT1988_15_2_LIMIT_DIFFERENCE,
    MANT1988_15_2_OPEN_DIFFERENCE,
    KURLANDER_BOWL_SINGLE_MOTIF
    //KURLANDER_BOWL_ALL_MOTIFS
};

class CsgSampleNamesUtil {
public:
    static const char* name(CsgSampleNames value);
    static CsgSampleNames nextCircular(CsgSampleNames value);
    static const char* getLabel(CsgSampleNames value);
    static CsgOperationNames getPreferredOperation(
        CsgSampleNames value, CsgOperationNames currentOperation);
    static int getDisplayIndex(CsgSampleNames value);
    static int getTotalSamples();
    /// Java: `values()[ordinal]`
    static CsgSampleNames fromOrdinal(int ordinal);

    /**
    Java: `CsgSampleNames.valueOf(name)`
    @return false when no value has that name (Java throws
    `IllegalArgumentException`)
    */
    static bool valueOf(const char* name, CsgSampleNames* out);

private:
    CsgSampleNamesUtil() {}
};

#endif
