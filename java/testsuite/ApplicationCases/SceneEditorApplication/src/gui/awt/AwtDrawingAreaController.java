package gui.awt;

// AWT/Swing classes
import java.awt.Component;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

// VSDK classes
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.gui.AwtSystem;
import vsdk.toolkit.gui.viewport.Viewport;

// Application classes
import model.DrawingArea;
import vsdk.toolkit.gui.viewport.ViewportSetInteractionListener;
import gui.DrawingAreaInteractionListener;
import gui.DrawingAreaInteractionTechniques;

/**
Maps the AWT events of the component presenting the drawing area to vitral
events, which are processed by the `DrawingAreaInteractionTechniques`. It also
owns the AWT services of the interaction: the projection location popup menu
and the focus of the component.
*/
public class AwtDrawingAreaController implements
    MouseListener, MouseMotionListener, KeyListener
{
    private final Component canvas;
    private final DrawingArea drawingArea;
    private final DrawingAreaInteractionTechniques techniques;
    private final DrawingAreaInteractionListener listener;
    private final AwtProjectionLocationPopup projectionLocationPopup;

    /**
    Creates the controller and starts listening to the events of the canvas.
    @param canvas component presenting the drawing area
    @param drawingArea state of the drawing area
    @param techniques techniques processing the (vitral) events
    @param listener receives the application level commands that depend on
    the raw AWT event (full screen toggle)
    */
    public AwtDrawingAreaController(Component canvas,
                                    DrawingArea drawingArea,
                                    DrawingAreaInteractionTechniques techniques,
                                    DrawingAreaInteractionListener listener)
    {
        this.canvas = canvas;
        this.drawingArea = drawingArea;
        this.techniques = techniques;
        this.listener = listener;

        projectionLocationPopup = new AwtProjectionLocationPopup(
            drawingArea.getViewportSet(), techniques.getViewportSetTechniques(), canvas);
        techniques.getViewportSetTechniques().setListener(new ViewportSetInteractionListener() {
            @Override
            public void projectionLocationMenuRequested(Viewport viewport, int x, int y) {
                projectionLocationPopup.show(viewport,
                    drawingArea.scaleXToCanvas(x), drawingArea.scaleYToCanvas(y));
            }
        });

        // TAB is a command of the drawing area (it cycles the boxes of the input gizmo of
        // the translation gizmo), not a request to move the focus
        canvas.setFocusTraversalKeysEnabled(false);
        canvas.addMouseListener(this);
        canvas.addMouseMotionListener(this);
        canvas.addKeyListener(this);
    }

    private void syncCanvasSize()
    {
        drawingArea.updateCanvasSize(canvas.getWidth(), canvas.getHeight());
    }

    /**
    Delivers a synthetic mouse event to the canvas, as if it came from the
    user's pointer. Intended for automated agents (see `AwtJogl4VitralEditorMCP`).
    Must be called from the event dispatch thread.
    @param type one of "move", "press", "drag", "release"
    @param x canvas (AWT) x coordinate
    @param y canvas (AWT) y coordinate
    @param button AWT button number (1 = left)
    */
    public void injectMouseEvent(String type, int x, int y, int button)
    {
        int id;
        int modifiers = 0;
        int buttonMask = MouseEvent.getMaskForButton(button);

        switch ( type ) {
            case "move" -> id = MouseEvent.MOUSE_MOVED;
            case "press" -> {
                id = MouseEvent.MOUSE_PRESSED;
                modifiers = buttonMask;
            }
            case "drag" -> {
                id = MouseEvent.MOUSE_DRAGGED;
                modifiers = buttonMask;
            }
            case "release" -> id = MouseEvent.MOUSE_RELEASED;
            default -> throw new IllegalArgumentException("Unknown mouse event type \"" +
                type + "\". Use move, press, drag or release");
        }
        MouseEvent event = new MouseEvent(
            canvas, id, System.currentTimeMillis(), modifiers, x, y, 1, false,
            type.equals("move") ? MouseEvent.NOBUTTON : button);

        canvas.dispatchEvent(event);
    }

    /**
    Delivers a synthetic key press to this controller, as if it came from the
    user's keyboard (whatever component has the focus). Intended for automated agents (see `AwtJogl4VitralEditorMCP`).
    Must be called from the event dispatch thread.
    @param key a single character (i.e. "5", "x") or one of the names "tab",
    "enter", "backspace", "escape", "left", "right", "up", "down", "pageup",
    "pagedown"
    @param shift true to press it with the SHIFT key down
    */
    public void injectKeyEvent(String key, boolean shift)
    {
        int keyCode;
        char keyChar;

        switch ( key ) {
            case "tab" -> {
                keyCode = KeyEvent.VK_TAB;
                keyChar = '\t';
            }
            case "enter" -> {
                keyCode = KeyEvent.VK_ENTER;
                keyChar = '\n';
            }
            case "backspace" -> {
                keyCode = KeyEvent.VK_BACK_SPACE;
                keyChar = '\b';
            }
            case "escape" -> {
                keyCode = KeyEvent.VK_ESCAPE;
                keyChar = KeyEvent.CHAR_UNDEFINED;
            }
            case "left", "right", "up", "down", "pageup", "pagedown" -> {
                keyCode = switch ( key ) {
                    case "left" -> KeyEvent.VK_LEFT;
                    case "right" -> KeyEvent.VK_RIGHT;
                    case "up" -> KeyEvent.VK_UP;
                    case "down" -> KeyEvent.VK_DOWN;
                    case "pageup" -> KeyEvent.VK_PAGE_UP;
                    default -> KeyEvent.VK_PAGE_DOWN;
                };
                keyChar = KeyEvent.CHAR_UNDEFINED;
            }
            default -> {
                if ( key.length() != 1 ) {
                    throw new IllegalArgumentException("Unknown key \"" + key +
                        "\". Use a single character, tab, enter, backspace, escape, left, right, up, down, pageup or pagedown");
                }
                keyChar = key.charAt(0);
                keyCode = KeyEvent.getExtendedKeyCodeForChar(keyChar);
            }
        }
        KeyEvent event = new KeyEvent(canvas, KeyEvent.KEY_PRESSED,
            System.currentTimeMillis(), shift ? KeyEvent.SHIFT_DOWN_MASK : 0,
            keyCode, keyChar);

        // Delivered directly: through the canvas, the AWT focus manager would
        // send it to the component that has the focus, if it is not the canvas
        keyPressed(event);
    }

    /**
    Projects a point of the scene to canvas (AWT) pixel coordinates using the
    active camera of a viewport.
    @param viewport viewport whose camera is used
    @param point point in world coordinates
    @return {x, y} in canvas pixels, or null if the point is behind the camera
    */
    public double[] projectToCanvas(Viewport viewport, Vector3Dd point)
    {
        syncCanvasSize();
        return drawingArea.projectToCanvas(viewport, point);
    }

    //= Mouse =============================================================

    @Override
    public void mouseEntered(MouseEvent e)
    {
        // While the projection location menu is open, it has the keyboard focus
        if ( !projectionLocationPopup.isVisible() ) {
            canvas.requestFocusInWindow();
        }
        syncCanvasSize();
        techniques.processMouseEnteredEvent(AwtSystem.awt2vsdkEvent(e));
    }

    @Override
    public void mouseExited(MouseEvent e)
    {
    }

    @Override
    public void mousePressed(MouseEvent e)
    {
        if ( projectionLocationPopup.consumesMouseEvent(e) ) {
            return;
        }
        syncCanvasSize();
        techniques.processMousePressedEvent(AwtSystem.awt2vsdkEvent(e));
    }

    @Override
    public void mouseReleased(MouseEvent e)
    {
        if ( projectionLocationPopup.consumesMouseEvent(e) ) {
            return;
        }
        syncCanvasSize();
        techniques.processMouseReleasedEvent(AwtSystem.awt2vsdkEvent(e));
    }

    @Override
    public void mouseClicked(MouseEvent e)
    {
        if ( projectionLocationPopup.consumesMouseEvent(e) ) {
            return;
        }
        syncCanvasSize();
        techniques.processMouseClickedEvent(AwtSystem.awt2vsdkEvent(e));
    }

    @Override
    public void mouseMoved(MouseEvent e)
    {
        vsdk.toolkit.gui.MouseEvent event = AwtSystem.awt2vsdkEvent(e);

        syncCanvasSize();
        if ( !projectionLocationPopup.isVisible() ) {
            techniques.updateModeCursor(event);
        }
        techniques.processMouseMovedEvent(event);
    }

    @Override
    public void mouseDragged(MouseEvent e)
    {
        if ( projectionLocationPopup.consumesMouseEvent(e) ) {
            return;
        }
        syncCanvasSize();
        techniques.processMouseDraggedEvent(AwtSystem.awt2vsdkEvent(e));
    }

    //= Keyboard ==========================================================

    @Override
    public void keyPressed(KeyEvent e)
    {
        techniques.processKeyPressedEvent(AwtSystem.awt2vsdkEvent(e));

        // Ctrl+Shift+F: the key identity is lost in the vitral event for
        // control characters, so the chord is detected here
        int modifiers = e.getModifiersEx();

        if ( (modifiers & KeyEvent.SHIFT_DOWN_MASK) != 0x0 &&
             (modifiers & KeyEvent.CTRL_DOWN_MASK) != 0x0 &&
             e.getKeyCode() == KeyEvent.VK_F ) {
            listener.fullScreenGuiToggleRequested();
        }
    }

    @Override
    public void keyReleased(KeyEvent e)
    {
        techniques.processKeyReleasedEvent(AwtSystem.awt2vsdkEvent(e));
    }

    /**
    Do NOT call your controller from the `keyTyped` method, or the controller
    will be invoked twice for each key. Call it only from the `keyPressed` and
    `keyReleased` method
    @param e
    */
    @Override
    public void keyTyped(KeyEvent e)
    {
    }
}
