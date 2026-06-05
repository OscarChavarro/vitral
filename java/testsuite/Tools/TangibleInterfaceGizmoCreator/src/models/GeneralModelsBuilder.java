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
            default -> cubeFixture.buildGizmoModel(5);
        };
    }
}
