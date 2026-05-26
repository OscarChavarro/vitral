package vsdk.toolkit.gui.visualAnalytics;

// VSDK classes
import vsdk.toolkit.environment.geometry.element.Ray;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.surface.InfinitePlane;
import vsdk.toolkit.gui.Controller;
import vsdk.toolkit.gui.MouseEvent;

public class PercentageWheelWidgetController extends Controller {
    
    private final PercentageWheelWidget controlledWidget;
    
    public PercentageWheelWidgetController(PercentageWheelWidget controlledWidget)
    {
        this.controlledWidget = controlledWidget;
    }
    
    public boolean processMousePressedEvent(MouseEvent e, Camera c)
    {
        int i;

        i = selectSector(e.getX(), e.getY(), c);
        
        if ( i == -1 ) {
            return false;
        }
        
        controlledWidget.setSelectedSector(i);

        return true;   
    }
    
    public boolean processMouseReleasedEvent(MouseEvent e)
    {
        return false;        
    }
    
    public boolean processMouseClickedEvent(MouseEvent e)
    {
        return false;        
    }
    
    public boolean processMouseMovedEvent(MouseEvent e, Camera c)
    {
        int i;

        i = selectSector(e.getX(), e.getY(), c);
        
        if ( i == -1 ) {
            return false;
        }
        
        controlledWidget.setHighligtedSector(i);

        return true;   
    }

    private int selectSector(int x, int y, Camera c) {
        int i;
        Vector3Dd p = controlledWidget.getPosition();
        Ray ray;
        c.updateVectors();
        ray = c.generateRay(x, y);
        InfinitePlane plane;
        plane = new InfinitePlane(new Vector3Dd(0, 0, 1), p);
        Ray hit = plane.doIntersection(ray);
        if ( hit == null ) {
            return -1;
        }

        Vector3Dd inPlane;
        double r;
        double angle;
        
        inPlane = hit.origin().add(hit.direction().multiply(hit.t())).subtract(p);
        r = inPlane.length();
        angle = Math.toDegrees(inPlane.obtainSphericalThetaAngle());

        r = r/controlledWidget.getScale();
        
        if ( r < controlledWidget.getInnerRadius() ) {
            // User has click inside label center, nothing happens
            return -1;
        }
        else if ( r > controlledWidget.getOuterRadius() +
                      controlledWidget.getBorderWidth() ) {
            // User has click outside wheel, nothing happens
            return -1;
        }

        int N;
        N = controlledWidget.getDataset().getDoubles().size();

        i = (int)Math.floor(angle * (((double)N) / 360.0));
        return i;
    }

    /**
     * @return the controlledWidget
     */
    public PercentageWheelWidget getControlledWidget() {
        return controlledWidget;
    }
   
}
