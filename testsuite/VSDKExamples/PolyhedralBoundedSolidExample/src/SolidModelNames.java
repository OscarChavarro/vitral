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
    CSG_TEST_PART_1(23),
    CSG_TEST_PART_2(24),
    CSG_TEST_PART_3(25),
    FEATURED_OBJECT(26),
    IMPORT_OR_FEATURED_OBJECT(27),
    CSG_MANT1988_15_2_HOLED_INTERSECTION(28),
    CSG_MANT1988_15_2_LIMIT_INTERSECTION(29),
    CSG_MANT1988_15_2_LIMIT_DIFFERENCE(30),
    CSG_MANT1988_15_2_OPEN_DIFFERENCE(31),
    CSG_KURLANDER_BOWL(32);

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
