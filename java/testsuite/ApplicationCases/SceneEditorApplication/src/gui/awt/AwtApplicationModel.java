package gui.awt;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
Swing widgets and services of the GUI. Technology independent GUI state is
kept in `model.GuiState`.
*/
public class AwtApplicationModel
{
    private JLabel statusMessage;
    private JPanel statusBarPanel;
    private AwtImageControlWindow imageControlWindow;
    private AwtSelectorDialog selectorDialog;
    private AwtCommandExecutor executor;
    private AwtButtonsPanel executorPanel;
    private JFrame mainWindowWidget;
    private String lookAndFeel;

    private AwtModifyPanel modifyPanel;

    public JLabel getStatusMessage()
    {
        return statusMessage;
    }

    public void setStatusMessage(JLabel statusMessage)
    {
        this.statusMessage = statusMessage;
    }

    public JPanel getStatusBarPanel()
    {
        return statusBarPanel;
    }

    public void setStatusBarPanel(JPanel statusBarPanel)
    {
        this.statusBarPanel = statusBarPanel;
    }

    public AwtImageControlWindow getImageControlWindow()
    {
        return imageControlWindow;
    }

    public void setImageControlWindow(AwtImageControlWindow imageControlWindow)
    {
        this.imageControlWindow = imageControlWindow;
    }

    public AwtSelectorDialog getSelectorDialog()
    {
        return selectorDialog;
    }

    public void setSelectorDialog(AwtSelectorDialog selectorDialog)
    {
        this.selectorDialog = selectorDialog;
    }

    public AwtCommandExecutor getExecutor()
    {
        return executor;
    }

    public void setExecutor(AwtCommandExecutor executor)
    {
        this.executor = executor;
    }

    public AwtButtonsPanel getExecutorPanel()
    {
        return executorPanel;
    }

    public void setExecutorPanel(AwtButtonsPanel executorPanel)
    {
        this.executorPanel = executorPanel;
    }

    public JFrame getMainWindowWidget()
    {
        return mainWindowWidget;
    }

    public void setMainWindowWidget(JFrame mainWindowWidget)
    {
        this.mainWindowWidget = mainWindowWidget;
    }

    public String getLookAndFeel()
    {
        return lookAndFeel;
    }

    public void setLookAndFeel(String lookAndFeel)
    {
        this.lookAndFeel = lookAndFeel;
    }

    public AwtModifyPanel getModifyPanel()
    {
        return modifyPanel;
    }

    public void setModifyPanel(AwtModifyPanel modifyPanel)
    {
        this.modifyPanel = modifyPanel;
    }
}
