package application.gui;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import vsdk.toolkit.gui.widget.Widget;

public class AwtApplicationModel
{
    private Widget gui;
    private JLabel statusMessage;
    private JPanel statusBarPanel;
    private SwingImageControlWindow imageControlWindow;
    private SwingSelectorDialog selectorDialog;
    private GUIEventExecutor executor;
    private ButtonsPanel executorPanel;
    private JFrame mainWindowWidget;
    private String lookAndFeel;
    private String languageGuiFile;
    private ModifyPanel modifyPanel;
    private boolean modifyPanelSelected;
    private boolean fullScreenGuiMode;
    private GUIEventExecutor guiEventExecutor;

    public Widget getGui()
    {
        return gui;
    }

    public void setGui(Widget gui)
    {
        this.gui = gui;
    }

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

    public SwingImageControlWindow getImageControlWindow()
    {
        return imageControlWindow;
    }

    public void setImageControlWindow(SwingImageControlWindow imageControlWindow)
    {
        this.imageControlWindow = imageControlWindow;
    }

    public SwingSelectorDialog getSelectorDialog()
    {
        return selectorDialog;
    }

    public void setSelectorDialog(SwingSelectorDialog selectorDialog)
    {
        this.selectorDialog = selectorDialog;
    }

    public GUIEventExecutor getExecutor()
    {
        return executor;
    }

    public void setExecutor(GUIEventExecutor executor)
    {
        this.executor = executor;
    }

    public ButtonsPanel getExecutorPanel()
    {
        return executorPanel;
    }

    public void setExecutorPanel(ButtonsPanel executorPanel)
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

    public String getLanguageGuiFile()
    {
        return languageGuiFile;
    }

    public void setLanguageGuiFile(String languageGuiFile)
    {
        this.languageGuiFile = languageGuiFile;
    }

    public ModifyPanel getModifyPanel()
    {
        return modifyPanel;
    }

    public void setModifyPanel(ModifyPanel modifyPanel)
    {
        this.modifyPanel = modifyPanel;
    }

    public boolean isModifyPanelSelected()
    {
        return modifyPanelSelected;
    }

    public void setModifyPanelSelected(boolean modifyPanelSelected)
    {
        this.modifyPanelSelected = modifyPanelSelected;
    }

    public boolean isFullScreenGuiMode()
    {
        return fullScreenGuiMode;
    }

    public void setFullScreenGuiMode(boolean fullScreenGuiMode)
    {
        this.fullScreenGuiMode = fullScreenGuiMode;
    }

    public void toggleFullScreenGuiMode()
    {
        fullScreenGuiMode = !fullScreenGuiMode;
    }

    public GUIEventExecutor getGuiEventExecutor()
    {
        return guiEventExecutor;
    }

    public void setGuiEventExecutor(GUIEventExecutor guiEventExecutor)
    {
        this.guiEventExecutor = guiEventExecutor;
    }
}
