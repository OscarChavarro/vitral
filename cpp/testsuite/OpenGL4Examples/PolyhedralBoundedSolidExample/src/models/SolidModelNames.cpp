#include <cstring>

#include "models/SolidModelNames.h"

static const SolidModelNames MAIN_SEQUENCE[] = {
    SolidModelNames::MVFS_SMEV_SAMPLE,
    SolidModelNames::BOX,
    SolidModelNames::HOLED_BOX,
    SolidModelNames::HOLLOW_BOX,
    SolidModelNames::ARC_SAMPLE,
    SolidModelNames::CIRCULAR_LAMINA,
    SolidModelNames::TRANSLATIONAL_SWEEP_EXTRUDE_FACE_PLANAR_ARC,
    SolidModelNames::TRANSLATIONAL_SWEEP_EXTRUDE_FACE_PLANAR_CIRCULAR,
    SolidModelNames::SPHERE,
    SolidModelNames::CONE,
    SolidModelNames::CYLINDER,
    SolidModelNames::CSG_LAMP_SHELL,
    SolidModelNames::ARROW,
    SolidModelNames::LAMINA_WITH_TWO_SHELLS,
    SolidModelNames::LAMINA_WITH_HOLE,
    SolidModelNames::FONT_BLOCK,
    SolidModelNames::GLUED_CYLINDERS,
    SolidModelNames::EULER_OPERATORS_TEST,
    SolidModelNames::ROTATIONAL_SWEEP,
    SolidModelNames::SPLIT_TEST_PART_1,
    SolidModelNames::SPLIT_TEST_PART_2,
    SolidModelNames::SPLIT_TEST_PART_3,
    SolidModelNames::CSG_DIRECT,
    SolidModelNames::CSG_OPERAND1_PARTIAL,
    SolidModelNames::CSG_OPERAND2_PARTIAL,
    SolidModelNames::FEATURED_OBJECT,
    SolidModelNames::IMPORT_OR_FEATURED_OBJECT,
    SolidModelNames::STEP_IMPORT
};

static const int MAIN_SEQUENCE_LENGTH =
    (int)(sizeof(MAIN_SEQUENCE) / sizeof(MAIN_SEQUENCE[0]));

/// Java: `values()`, in declaration order
static const char* const NAMES[] = {
    "MVFS_SMEV_SAMPLE",
    "BOX",
    "HOLED_BOX",
    "HOLLOW_BOX",
    "ARC_SAMPLE",
    "CIRCULAR_LAMINA",
    "TRANSLATIONAL_SWEEP_EXTRUDE_FACE_PLANAR_ARC",
    "TRANSLATIONAL_SWEEP_EXTRUDE_FACE_PLANAR_CIRCULAR",
    "SPHERE",
    "CONE",
    "CYLINDER",
    "CSG_MOON_BLOCK",
    "CSG_LAMP_SHELL",
    "ARROW",
    "LAMINA_WITH_TWO_SHELLS",
    "LAMINA_WITH_HOLE",
    "FONT_BLOCK",
    "GLUED_CYLINDERS",
    "EULER_OPERATORS_TEST",
    "ROTATIONAL_SWEEP",
    "SPLIT_TEST_PART_1",
    "SPLIT_TEST_PART_2",
    "SPLIT_TEST_PART_3",
    "CSG_DIRECT",
    "CSG_OPERAND1_PARTIAL",
    "CSG_OPERAND2_PARTIAL",
    "FEATURED_OBJECT",
    "IMPORT_OR_FEATURED_OBJECT",
    "STEP_IMPORT"
};

static const int VALUES_LENGTH = (int)(sizeof(NAMES) / sizeof(NAMES[0]));

const char* SolidModelNamesUtil::name(SolidModelNames value)
{
    return NAMES[(int)value];
}

SolidModelNames SolidModelNamesUtil::fromId(int id)
{
    int i;
    for ( i = 0; i < VALUES_LENGTH; i++ ) {
        if ( i == id ) {
            return (SolidModelNames)i;
        }
    }
    return SolidModelNames::HOLED_BOX;
}

SolidModelNames SolidModelNamesUtil::nextClamped(SolidModelNames value)
{
    int currentIndex = getMainSequenceIndex(value);
    if ( currentIndex < 0 ) {
        return MAIN_SEQUENCE[0];
    }

    int nextIndex = currentIndex + 1;
    if ( nextIndex >= MAIN_SEQUENCE_LENGTH ) {
        nextIndex = MAIN_SEQUENCE_LENGTH - 1;
    }
    return MAIN_SEQUENCE[nextIndex];
}

SolidModelNames SolidModelNamesUtil::previousClamped(SolidModelNames value)
{
    int currentIndex = getMainSequenceIndex(value);
    if ( currentIndex < 0 ) {
        return MAIN_SEQUENCE[0];
    }

    int previousIndex = currentIndex - 1;
    if ( previousIndex < 0 ) {
        previousIndex = 0;
    }
    return MAIN_SEQUENCE[previousIndex];
}

int SolidModelNamesUtil::getDisplayIndex(SolidModelNames value)
{
    int index = getMainSequenceIndex(value);
    if ( index < 0 ) {
        return 1;
    }
    return index + 1;
}

int SolidModelNamesUtil::getTotalModels()
{
    return MAIN_SEQUENCE_LENGTH;
}

bool SolidModelNamesUtil::usesCsgDebugControls(SolidModelNames value)
{
    return value == SolidModelNames::CSG_MOON_BLOCK ||
        value == SolidModelNames::CSG_DIRECT ||
        value == SolidModelNames::CSG_OPERAND1_PARTIAL ||
        value == SolidModelNames::CSG_OPERAND2_PARTIAL;
}

int SolidModelNamesUtil::getMainSequenceIndex(SolidModelNames value)
{
    int i;

    for ( i = 0; i < MAIN_SEQUENCE_LENGTH; i++ ) {
        if ( MAIN_SEQUENCE[i] == value ) {
            return i;
        }
    }
    return -1;
}

bool SolidModelNamesUtil::valueOf(const char* name, SolidModelNames* out)
{
    int i;
    for ( i = 0; i < VALUES_LENGTH; i++ ) {
        if ( std::strcmp(NAMES[i], name) == 0 ) {
            *out = (SolidModelNames)i;
            return true;
        }
    }
    return false;
}
