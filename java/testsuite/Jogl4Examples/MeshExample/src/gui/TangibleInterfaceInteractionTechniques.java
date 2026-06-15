package gui;

import vsdk.toolkit.gui.tangibleInterfaces.TangibleInterfaceEvent;
import vsdk.toolkit.gui.tangibleInterfaces.TangibleInterfaceListener;

/**
First interaction technique connecting MeshExample to the tangible interface
marker tracking service. As a starting point, every received
`TangibleInterfaceEvent` is simply printed to the console.
*/
public class TangibleInterfaceInteractionTechniques implements TangibleInterfaceListener {
    public void tangibleInterfaceEventReceived(TangibleInterfaceEvent event) {
        System.out.println("Tangible interface event received: " + event);
    }
}
