package application.commands;

/**
What the GUI technology presents for the `GuiEventExecutor`.
*/
public interface Presenter
{
    /**
    @param message text to show in the status bar
    */
    void showStatusMessage(String message);
}
