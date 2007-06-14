package vsdk.external.jogl;

import vsdk.framework.presentation.panels.PresentationPanel;

public class JoglFactory
{
    public static PresentationPanel createPresentationPanel()
    {
        return new JoglPresentationPanel();
    }
}
