package gui;

import vsdk.toolkit.gui.tangibleInterfaces.TangibleInterfaceEvent;
import vsdk.toolkit.gui.tangibleInterfaces.TangibleInterfaceEvent2RayGizmoMapper;
import vsdk.toolkit.gui.tangibleInterfaces.TangibleInterfaceListener;
import model.MeshModel;

public class TangibleInterfaceInteractionTechniques implements TangibleInterfaceListener {
    private static final String RAY_CUBE_TANGIBLE_ELEMENT_ID = "rayCube";

    private final MeshModel model;
    private final Runnable repaintCallback;
    private final TangibleInterfaceEvent2RayGizmoMapper mapper;

    public TangibleInterfaceInteractionTechniques(MeshModel model, Runnable repaintCallback) {
        this.model = model;
        this.repaintCallback = repaintCallback;
        this.mapper = new TangibleInterfaceEvent2RayGizmoMapper(model.getCamera());
    }

    @Override
    public void tangibleInterfaceEventReceived(TangibleInterfaceEvent event) {
        if ( event == null ) {
            return;
        }

        if ( !RAY_CUBE_TANGIBLE_ELEMENT_ID.equals(event.getId()) ) {
            return;
        }

        mapper.map(event, model.getRayGizmo());
        repaintCallback.run();
    }
}
