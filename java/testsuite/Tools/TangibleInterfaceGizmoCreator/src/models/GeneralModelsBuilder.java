package models;

import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;

public final class GeneralModelsBuilder
{
    private GeneralModelsBuilder()
    {
    }

    public static PolyhedralBoundedSolid buildSolid(TangibleInterfaceGizmosModel model)
    {
        TangibleInterfaceCubeFixture cubeFixture = new TangibleInterfaceCubeFixture();
        return switch (model.getSolidModelName()) {
            case CUBE_PART_1 -> cubeFixture.buildGizmoModel(0);
            case CUBE_PART_2 -> cubeFixture.buildGizmoModel(1);
            case CUBE_PART_3 -> cubeFixture.buildGizmoModel(2);
            case CUBE_PART_4 -> cubeFixture.buildGizmoModel(3);
            case CUBE_PART_5 -> cubeFixture.buildGizmoModel(4);
            case CUBE_PART_6 -> cubeFixture.buildGizmoModel(7);
            case CUBE_STICK_BASE -> cubeFixture.buildGizmoModel(5,
                model.getInnerRadius(), model.getOuterRadius(), model.getBaseHeight());
            case CUBE_STICK_HOLED -> cubeFixture.buildGizmoModel(6,
                model.getInnerRadius(), model.getOuterRadius(), model.getBaseHeight());
            case STEPER_MOTOR_GUIDE -> cubeFixture.buildGizmoModel(8,
                model.getInnerRadius(), model.getOuterRadius(), model.getBaseHeight());
        };
    }
}
