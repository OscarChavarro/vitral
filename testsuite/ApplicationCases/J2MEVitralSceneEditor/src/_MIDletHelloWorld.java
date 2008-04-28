//===========================================================================
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Command;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

public class _MIDletHelloWorld extends MIDlet implements CommandListener {
    /** This MIDlet's Display object */
    private Display display; // Our display

    /** The canvas widget */
    private MidletCanvas canvas;

    /** GUI menu and options commands */
    private Command exitCommand = new Command("Exit", Command.EXIT, 1);
    private Command aboutCommand = new Command("Menu", Command.HELP, 30);
    private Command command1 = new Command("Test option 1", Command.SCREEN, 7);
    private Command command2 = new Command("Test option 2", Command.SCREEN, 8);

    /**
    Construct a new MIDlet and initialize.
    */
    public _MIDletHelloWorld() {
        display = Display.getDisplay(this);
        canvas = new MidletCanvas(display.isColor(), display.numColors());

        canvas.addCommand(exitCommand);
        canvas.addCommand(aboutCommand);
        canvas.addCommand(command1);
        canvas.addCommand(command2);
        canvas.setCommandListener(this);
    }

    /**
    Create the Colorcanvas and make it current
    */
    public void startApp() {
        display.setCurrent(canvas);
    }

    /**
    Pause
    */
    public void pauseApp() {
        ;
    }

    /**
    Destroy must cleanup everything.
    @param unconditional true if must destroy
    */
    public void destroyApp(boolean unconditional) {
        ;
    }

    /**
    Respond to commands issued on any Screen.
    @param c Command invoked
    @param s Displayable on which the command was invoked
    */
    public void commandAction(Command c, Displayable s) {
        if ( c == exitCommand ) {
            destroyApp(true);
            notifyDestroyed();
          }
          else if ( c == aboutCommand ) {
            About.showAbout(display);
          }
          else if ( c == command1 ) {
            //canvas.removeCommand(command1);
            //canvas.addCommand(command2);
          }
          else if ( c == command2 ) {
            //canvas.removeCommand(command2);
            //canvas.addCommand(command1);
          }
        ;
    }
        
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
