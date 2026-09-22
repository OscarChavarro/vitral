package gui.awt;

// Java AWT/Swing classes
import javax.swing.JFrame;

/**
Executes the commands issued by the buttons of the AWT/Swing GUI.
*/
public interface AwtCommandExecutor
{
    /**
    Folders proposed by file dialogs are kept in `model.GuiState`.
    @param label identifier of the command
    @param mainWindowWidget main window, parent of dialogs
    @return true if the command was recognized
    */
    boolean executeCommand(String label, JFrame mainWindowWidget);
}
