package gui.awt;

// AWT classes
import java.awt.EventQueue;
import java.awt.Toolkit;

/**
Coalesces the requests to draw the drawing area, so a frame is drawn only
when the event dispatch thread has nothing else to do: if 20 events arrive
while a frame is being drawn, the 20 are processed (they only change the
state of the model, which is fast) and then one frame is drawn, presenting
the state left by the last one, instead of one frame for each event.

Requests are attended in the event dispatch thread. While there are events
waiting in the queue, the frame is postponed after them; the number of
postponements is bounded, so a continuous stream of events (i.e. the pointer
always moving) still gets frames.

Frames requested by the window system (when the canvas is exposed) do not
pass through here: AWT draws them directly.
*/
public class AwtFrameScheduler
{
    /// Times a frame gives way to waiting events before being drawn anyway
    private static final int MAXIMUM_POSTPONEMENTS = 64;

    private final Runnable drawFrame;
    private boolean scheduled;
    private int postponements;

    /**
    @param drawFrame draws a frame immediately, in the event dispatch thread
    */
    public AwtFrameScheduler(Runnable drawFrame)
    {
        this.drawFrame = drawFrame;
        scheduled = false;
        postponements = 0;
    }

    /**
    Requests a frame. It can be called from any thread: the frame is drawn
    later, in the event dispatch thread, once for all the requests done
    before it is drawn.
    */
    public void requestFrame()
    {
        synchronized ( this ) {
            if ( scheduled ) {
                return;
            }
            scheduled = true;
        }
        EventQueue.invokeLater(this::attend);
    }

    private void attend()
    {
        if ( postponements < MAXIMUM_POSTPONEMENTS && areEventsWaiting() ) {
            postponements++;
            EventQueue.invokeLater(this::attend);
            return;
        }
        synchronized ( this ) {
            scheduled = false;
        }
        postponements = 0;
        // Requests done while drawing schedule the next frame
        drawFrame.run();
    }

    private static boolean areEventsWaiting()
    {
        try {
            return Toolkit.getDefaultToolkit().getSystemEventQueue().peekEvent() != null;
        }
        catch ( SecurityException e ) {
            return false;
        }
    }
}
