package gui;

import vsdk.toolkit.gui.tangibleInterfaces.TangibleInterfaceEvent;
import vsdk.toolkit.gui.tangibleInterfaces.TangibleInterfaceEvent2InfinitePlaneGizmoMapper;
import vsdk.toolkit.gui.tangibleInterfaces.TangibleInterfaceEvent2RayGizmoMapper;
import vsdk.toolkit.gui.tangibleInterfaces.TangibleInterfaceListener;
import model.SolidTextureModel;

public class TangibleInterfaceInteractionTechniques implements TangibleInterfaceListener {
    private static final String RAY_CUBE_TANGIBLE_ELEMENT_ID = "rayCube1";
    private static final String CUTTING_PLANE_CUBE_TANGIBLE_ELEMENT_ID = "cuttingPlane1";

    private final SolidTextureModel model;
    private final Runnable repaintCallback;
    private final TangibleInterfaceEvent2RayGizmoMapper toRayGizmoMapper;
    private final TangibleInterfaceEvent2InfinitePlaneGizmoMapper toInfinitePlaneGizmoMapper;

    public TangibleInterfaceInteractionTechniques(SolidTextureModel model, Runnable repaintCallback) {
        this.model = model;
        this.repaintCallback = repaintCallback;
        this.toRayGizmoMapper = new TangibleInterfaceEvent2RayGizmoMapper(model.getCamera());
        this.toInfinitePlaneGizmoMapper =
            new TangibleInterfaceEvent2InfinitePlaneGizmoMapper(model.getCamera());
    }

    @Override
    public void tangibleInterfaceEventReceived(TangibleInterfaceEvent event) {
        if ( event == null ) {
            return;
        }

        if ( RAY_CUBE_TANGIBLE_ELEMENT_ID.equals(event.getId()) ) {
            toRayGizmoMapper.map(event, model.getRayGizmo());
        } else if ( CUTTING_PLANE_CUBE_TANGIBLE_ELEMENT_ID.equals(event.getId()) ) {
            toInfinitePlaneGizmoMapper.map(event, model.getInfinitePlaneGizmo());
        }

        repaintCallback.run();
    }
}
