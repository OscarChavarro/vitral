package models;

public enum GizmoNames
{
    CUBE_PART_1(1),
    CUBE_PART_2(2),
    CUBE_PART_3(3),
    CUBE_PART_4(4),
    CUBE_PART_5(5),
    CUBE_PART_6(6),
    CUBE_STICK_BASE(7),
    CUBE_STICK_HOLED(8);

    private final int id;

    GizmoNames(int id)
    {
        this.id = id;
    }

    public static GizmoNames fromId(int id)
    {
        for ( GizmoNames value : values() ) {
            if ( value.id == id ) {
                return value;
            }
        }
        return CUBE_PART_1;
    }

    public GizmoNames nextClamped()
    {
        GizmoNames[] all = values();
        int nextIndex = ordinal() + 1;
        if ( nextIndex >= all.length ) {
            nextIndex = all.length - 1;
        }
        return all[nextIndex];
    }

    public GizmoNames previousClamped()
    {
        GizmoNames[] all = values();
        int previousIndex = ordinal() - 1;
        if ( previousIndex < 0 ) {
            previousIndex = 0;
        }
        return all[previousIndex];
    }

    public int getDisplayIndex()
    {
        return ordinal() + 1;
    }

    public static int getTotalModels()
    {
        return values().length;
    }
}
