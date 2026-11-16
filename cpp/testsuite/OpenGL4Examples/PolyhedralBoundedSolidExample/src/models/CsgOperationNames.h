#ifndef __POLYHEDRAL_EXAMPLE_CSG_OPERATION_NAMES__
#define __POLYHEDRAL_EXAMPLE_CSG_OPERATION_NAMES__

/// C++ port of Java enum `models.CsgOperationNames`
enum class CsgOperationNames {
    UNION,
    INTERSECTION,
    DIFFERENCE_A_MINUS_B,
    DIFFERENCE_B_MINUS_A
};

class CsgOperationNamesUtil {
public:
    static const char* name(CsgOperationNames value);
    static CsgOperationNames nextCircular(CsgOperationNames value);
    static const char* getLabel(CsgOperationNames value);

private:
    CsgOperationNamesUtil() {}
};

#endif
