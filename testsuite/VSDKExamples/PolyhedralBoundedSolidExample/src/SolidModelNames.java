public enum SolidModelNames
{
    MVFS_SMEV_SAMPLE(0),
    CREATE_BOX(1),
    CREATE_HOLED_BOX(2),
    ADD_ARC_SAMPLE(3),
    CREATE_CIRCULAR_LAMINA(4),
    TRANSLATIONAL_SWEEP_EXTRUDE_FACE_PLANAR_ARC(5),
    TRANSLATIONAL_SWEEP_EXTRUDE_FACE_PLANAR_CIRCULAR(6),
    CREATE_SPHERE(7),
    CREATE_CONE(8),
    CREATE_ARROW(9),
    CREATE_LAMINA_WITH_TWO_SHELLS(10),
    CREATE_LAMINA_WITH_HOLE(11),
    CREATE_FONT_BLOCK(12),
    CREATE_GLUED_CILINDERS(13),
    EULER_OPERATORS_TEST(14),
    ROTATIONAL_SWEEP_TEST(15),
    SPLIT_TEST_PART_1(16),
    SPLIT_TEST_PART_2(17),
    SPLIT_TEST_PART_3(18),
    CSG_TEST_PART_1(19),
    CSG_TEST_PART_2(20),
    CSG_TEST_PART_3(21),
    FEATURED_OBJECT(22),
    IMPORT_OR_FEATURED_OBJECT(23);

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
        return CREATE_HOLED_BOX;
    }

    public SolidModelNames nextCircular()
    {
        int nextId = (id + 1) % 24;
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
