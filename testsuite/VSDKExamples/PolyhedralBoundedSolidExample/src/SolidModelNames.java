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
    ARROW(12),
    LAMINA_WITH_TWO_SHELLS(13),
    LAMINA_WITH_HOLE(14),
    FONT_BLOCK(15),
    GLUED_CILINDERS(16),
    EULER_OPERATORS_TEST(17),
    ROTATIONAL_SWEEP(18),
    SPLIT_TEST_PART_1(19),
    SPLIT_TEST_PART_2(20),
    SPLIT_TEST_PART_3(21),
    CSG_TEST_PART_1(22),
    CSG_TEST_PART_2(23),
    CSG_TEST_PART_3(24),
    FEATURED_OBJECT(25),
    IMPORT_OR_FEATURED_OBJECT(26),
    CSG_MANT1988_15_2_HOLED_INTERSECTION(27),
    CSG_MANT1988_15_2_LIMIT_INTERSECTION(28),
    CSG_MANT1988_15_2_LIMIT_DIFFERENCE(29),
    CSG_MANT1988_15_2_OPEN_DIFFERENCE(30),
    CSG_KURLANDER_BOWL(31);

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
