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
    ARROW(10),
    LAMINA_WITH_TWO_SHELLS(11),
    LAMINA_WITH_HOLE(12),
    FONT_BLOCK(13),
    GLUED_CILINDERS(14),
    EULER_OPERATORS_TEST(15),
    ROTATIONAL_SWEEP(16),
    SPLIT_TEST_PART_1(17),
    SPLIT_TEST_PART_2(18),
    SPLIT_TEST_PART_3(19),
    CSG_TEST_PART_1(20),
    CSG_TEST_PART_2(21),
    CSG_TEST_PART_3(22),
    FEATURED_OBJECT(23),
    IMPORT_OR_FEATURED_OBJECT(24),
    CSG_MANT1988_15_2_HOLED_INTERSECTION(25),
    CSG_MANT1988_15_2_LIMIT_INTERSECTION(26),
    CSG_MANT1988_15_2_LIMIT_DIFFERENCE(27),
    CSG_MANT1988_15_2_OPEN_DIFFERENCE(28),
    CSG_KURLANDER_BOWL(29);

    private final int id;

    SolidModelNames(int id)
    {
        this.id = id;
    }

    public int getId()
    {
        return id;
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

    public SolidModelNames nextCircular()
    {
        int nextId = (id + 1) % values().length;
        return fromId(nextId);
    }

    public SolidModelNames previousClamped()
    {
        int previousId = id - 1;
        if ( previousId < 0 ) {
            previousId = 0;
        }
        return fromId(previousId);
    }
}
