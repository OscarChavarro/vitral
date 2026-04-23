package models;

public enum SolidModelNames
{
    MVFS_SMEV_SAMPLE(0),
    BOX(1),
    HOLED_BOX(2),
    HOLLOW_BOX(3),
    ARC_SAMPLE(4),
    CIRCULAR_LAMINA(5),
    TRANSLATIONAL_SWEEP_EXTRUDE_FACE_PLANAR_ARC(6),
    TRANSLATIONAL_SWEEP_EXTRUDE_FACE_PLANAR_CIRCULAR(7),
    SPHERE(8),
    CONE(9),
    CYLINDER(10),
    CSG_MOON_BLOCK(11),
    CSG_LAMP_SHELL(12),
    ARROW(13),
    LAMINA_WITH_TWO_SHELLS(14),
    LAMINA_WITH_HOLE(15),
    FONT_BLOCK(16),
    GLUED_CYLINDERS(17),
    EULER_OPERATORS_TEST(18),
    ROTATIONAL_SWEEP(19),
    SPLIT_TEST_PART_1(20),
    SPLIT_TEST_PART_2(21),
    SPLIT_TEST_PART_3(22),
    CSG_DIRECT(23),
    CSG_OPERAND1_PARTIAL(24),
    CSG_OPERAND2_PARTIAL(25),
    FEATURED_OBJECT(26),
    IMPORT_OR_FEATURED_OBJECT(27),
    CSG_KURLANDER_BOWL(32);

    private static final SolidModelNames[] MAIN_SEQUENCE = {
        MVFS_SMEV_SAMPLE,
        BOX,
        HOLED_BOX,
        HOLLOW_BOX,
        ARC_SAMPLE,
        CIRCULAR_LAMINA,
        TRANSLATIONAL_SWEEP_EXTRUDE_FACE_PLANAR_ARC,
        TRANSLATIONAL_SWEEP_EXTRUDE_FACE_PLANAR_CIRCULAR,
        SPHERE,
        CONE,
        CYLINDER,
        CSG_LAMP_SHELL,
        ARROW,
        LAMINA_WITH_TWO_SHELLS,
        LAMINA_WITH_HOLE,
        FONT_BLOCK,
        GLUED_CYLINDERS,
        EULER_OPERATORS_TEST,
        ROTATIONAL_SWEEP,
        SPLIT_TEST_PART_1,
        SPLIT_TEST_PART_2,
        SPLIT_TEST_PART_3,
            CSG_DIRECT,
            CSG_OPERAND1_PARTIAL,
            CSG_OPERAND2_PARTIAL,
        FEATURED_OBJECT,
        IMPORT_OR_FEATURED_OBJECT,
        CSG_KURLANDER_BOWL
    };

    private final int id;

    SolidModelNames(int id)
    {
        this.id = id;
    }

    public static SolidModelNames fromId(int id)
    {
        for ( SolidModelNames value : values() ) {
            if ( value.id == id ) {
                return value;
            }
        }
        return HOLED_BOX;
    }

    public SolidModelNames nextClamped()
    {
        int currentIndex = getMainSequenceIndex();
        if ( currentIndex < 0 ) {
            return MAIN_SEQUENCE[0];
        }

        int nextIndex = currentIndex + 1;
        if ( nextIndex >= MAIN_SEQUENCE.length ) {
            nextIndex = MAIN_SEQUENCE.length - 1;
        }
        return MAIN_SEQUENCE[nextIndex];
    }

    public SolidModelNames previousClamped()
    {
        int currentIndex = getMainSequenceIndex();
        if ( currentIndex < 0 ) {
            return MAIN_SEQUENCE[0];
        }

        int previousIndex = currentIndex - 1;
        if ( previousIndex < 0 ) {
            previousIndex = 0;
        }
        return MAIN_SEQUENCE[previousIndex];
    }

    public int getDisplayIndex()
    {
        int index = getMainSequenceIndex();
        if ( index < 0 ) {
            return 1;
        }
        return index + 1;
    }

    public static int getTotalModels()
    {
        return MAIN_SEQUENCE.length;
    }

    public boolean usesCsgDebugControls()
    {
        return this == CSG_MOON_BLOCK ||
            this == CSG_DIRECT ||
            this == CSG_OPERAND1_PARTIAL ||
            this == CSG_OPERAND2_PARTIAL;
    }

    private int getMainSequenceIndex()
    {
        int i;

        for ( i = 0; i < MAIN_SEQUENCE.length; i++ ) {
            if ( MAIN_SEQUENCE[i] == this ) {
                return i;
            }
        }
        return -1;
    }
}
