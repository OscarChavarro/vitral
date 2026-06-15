package vsdk.toolkit.gui.tangibleInterfaces;

/**
This interface represents the listener side of the emitter/listener pattern
implemented by `TangibleInterfaceNetworkClient`. Implementors are notified
whenever a new 6-DoF pose update is received from the tangible interface
marker tracking service.
*/
public interface TangibleInterfaceListener {
    /**
    Called when a new pose update is received from the tangible interface
    marker tracking service.

    @param event the received pose update
    */
    void tangibleInterfaceEventReceived(TangibleInterfaceEvent event);
}
