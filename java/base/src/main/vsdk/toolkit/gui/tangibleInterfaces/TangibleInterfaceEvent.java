package vsdk.toolkit.gui.tangibleInterfaces;

import vsdk.toolkit.common.linealAlgebra.Quaterniond;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.gui.PresentationElement;

/**
This class represents a single 6-DoF pose update reported by a tangible
interface marker tracking service (see `TangibleInterfaceNetworkClient`),
identifying a marker group, its position and its rotation.
*/
public class TangibleInterfaceEvent extends PresentationElement {
    private String id;
    private Vector3Dd position;
    private Quaterniond rotation;

    /**
    Builds a tangible interface event.

    @param id identifier (label) of the marker group reporting the pose
    @param position position of the marker group, in meters
    @param rotation orientation of the marker group
    */
    public TangibleInterfaceEvent(String id, Vector3Dd position, Quaterniond rotation) {
        this.id = id;
        this.position = position;
        this.rotation = rotation;
    }

    /**
    @return identifier (label) of the marker group reporting the pose
    */
    public String getId() {
        return id;
    }

    /**
    @return position of the marker group, in meters
    */
    public Vector3Dd getPosition() {
        return position;
    }

    /**
    @return orientation of the marker group
    */
    public Quaterniond getRotation() {
        return rotation;
    }

    public String toString() {
        return "TangibleInterfaceEvent{id=" + id + ", position=" + position + ", rotation=" + rotation + "}";
    }
}
