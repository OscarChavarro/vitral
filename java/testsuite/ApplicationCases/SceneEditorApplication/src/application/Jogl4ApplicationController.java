package application;

import java.awt.Component;

import javax.swing.JLabel;

import application.model.ApplicationModel;
import application.render.jogl.JoglDrawingArea;

public class Jogl4ApplicationController
{
    private final ApplicationModel model;
    private JoglDrawingArea drawingArea;

    public Jogl4ApplicationController(ApplicationModel model)
    {
        this.model = model;
    }

    public JoglDrawingArea getDrawingArea()
    {
        return drawingArea;
    }

    public JoglDrawingArea getOrCreateDrawingArea(JLabel statusMessage,
                                                  SceneEditorApplication application)
    {
        if ( drawingArea == null ) {
            drawingArea = new JoglDrawingArea(model, statusMessage, application);
        }
        return drawingArea;
    }

    public Component getCanvas(JLabel statusMessage,
                               SceneEditorApplication application)
    {
        return getOrCreateDrawingArea(statusMessage, application).getCanvas();
    }

    public void requestFocusInWindow()
    {
        if ( drawingArea != null ) {
            drawingArea.getCanvas().requestFocusInWindow();
        }
    }

    public void repaint()
    {
        if ( drawingArea != null ) {
            drawingArea.getCanvas().repaint();
        }
    }
}
