package gui.awt;

import java.awt.Component;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;

import vsdk.toolkit.gui.viewport.ViewportElementScaler;

/**
AWT specific part of `ViewportElementScaler`: it finds out the resolution of the
screen using AWT, and informs it to the (platform independent) scaler injected
in the constructor. Other platforms should provide an equivalent class.

It is kept apart from the platform independent scaler because AWT is the only
way to know the screen in this platform.

The resolution informed is in physical pixels, which are the ones text is
drawn with: the screen bounds (that AWT reports in logical units) multiplied by
the scale of the screen's default transformation (i.e. 2 in HiDPI displays).
*/
public class AwtViewportElementScaler
{
    private final ViewportElementScaler elementScaler;

    public AwtViewportElementScaler(ViewportElementScaler elementScaler)
    {
        this.elementScaler = elementScaler;
    }

    public ViewportElementScaler getElementScaler()
    {
        return elementScaler;
    }

    /**
    Informs the scaler the resolution of the system's default screen. Nothing
    is done in headless environments.
    */
    public void updateFromDefaultScreen()
    {
        if ( GraphicsEnvironment.isHeadless() ) {
            return;
        }
        updateFrom(GraphicsEnvironment.getLocalGraphicsEnvironment().
            getDefaultScreenDevice().getDefaultConfiguration());
    }

    /**
    Informs the scaler the resolution of the screen where a component is
    shown (which can change if the component is moved to other screen or the
    resolution is changed). If the component is not displayable yet, the
    default screen is used.
    @param component
    */
    public void updateFromComponent(Component component)
    {
        GraphicsConfiguration configuration = null;

        if ( component != null ) {
            configuration = component.getGraphicsConfiguration();
        }
        if ( configuration == null ) {
            updateFromDefaultScreen();
        }
        else {
            updateFrom(configuration);
        }
    }

    private void updateFrom(GraphicsConfiguration configuration)
    {
        Rectangle bounds = configuration.getBounds();
        AffineTransform transform = configuration.getDefaultTransform();

        elementScaler.setScreenResolution(
            (int)Math.round(bounds.width * transform.getScaleX()),
            (int)Math.round(bounds.height * transform.getScaleY()));
    }
}
