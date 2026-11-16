#ifndef __POLYHEDRAL_EXAMPLE_SOLID_MODEL_NAMES__
#define __POLYHEDRAL_EXAMPLE_SOLID_MODEL_NAMES__

/**
C++ port of Java enum `models.SolidModelNames`: the identifier is the
enum value, and the methods are in `SolidModelNamesUtil`.
*/
enum class SolidModelNames {
    MVFS_SMEV_SAMPLE = 0,
    BOX = 1,
    HOLED_BOX = 2,
    HOLLOW_BOX = 3,
    ARC_SAMPLE = 4,
    CIRCULAR_LAMINA = 5,
    TRANSLATIONAL_SWEEP_EXTRUDE_FACE_PLANAR_ARC = 6,
    TRANSLATIONAL_SWEEP_EXTRUDE_FACE_PLANAR_CIRCULAR = 7,
    SPHERE = 8,
    CONE = 9,
    CYLINDER = 10,
    CSG_MOON_BLOCK = 11,
    CSG_LAMP_SHELL = 12,
    ARROW = 13,
    LAMINA_WITH_TWO_SHELLS = 14,
    LAMINA_WITH_HOLE = 15,
    FONT_BLOCK = 16,
    GLUED_CYLINDERS = 17,
    EULER_OPERATORS_TEST = 18,
    ROTATIONAL_SWEEP = 19,
    SPLIT_TEST_PART_1 = 20,
    SPLIT_TEST_PART_2 = 21,
    SPLIT_TEST_PART_3 = 22,
    CSG_DIRECT = 23,
    CSG_OPERAND1_PARTIAL = 24,
    CSG_OPERAND2_PARTIAL = 25,
    FEATURED_OBJECT = 26,
    IMPORT_OR_FEATURED_OBJECT = 27,
    STEP_IMPORT = 28
};

class SolidModelNamesUtil {
public:
    static const char* name(SolidModelNames value);
    static SolidModelNames fromId(int id);
    static SolidModelNames nextClamped(SolidModelNames value);
    static SolidModelNames previousClamped(SolidModelNames value);
    static int getDisplayIndex(SolidModelNames value);
    static int getTotalModels();
    static bool usesCsgDebugControls(SolidModelNames value);

    /**
    Java: `SolidModelNames.valueOf(name)`
    @return false when no value has that name (Java throws
    `IllegalArgumentException`)
    */
    static bool valueOf(const char* name, SolidModelNames* out);

private:
    static int getMainSequenceIndex(SolidModelNames value);
    SolidModelNamesUtil() {}
};

#endif
