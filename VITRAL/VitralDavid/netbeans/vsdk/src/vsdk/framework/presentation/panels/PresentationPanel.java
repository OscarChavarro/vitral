package vsdk.framework.presentation.panels;

import vsdk.framework.render.RenderService;

import vsdk.toolkit.common.QualitySelection;

public interface PresentationPanel
{
    public void setDisplayMode(String dispMode);
    
    public String[] getDisplayModes();
    
    public void setQualitySelection(QualitySelection qs);

    public QualitySelection getQualitySelection();
    
    public void displayData();
    
    public RenderService getRenderService();
}
