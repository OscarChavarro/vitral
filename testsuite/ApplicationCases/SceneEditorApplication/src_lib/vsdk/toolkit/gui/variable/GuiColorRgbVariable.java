/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vsdk.toolkit.gui.variable;

/**
 *
 * @author TaakeSlottet
 */
public class GuiColorRgbVariable extends GuiVariable {

    @Override
    public String getType() {
        return "ColorRgb";
    }

    public GuiColorRgbVariable() {
        super();
        this.validRange = "<[0.0, 1.0], [0.0, 1.0], [0.0, 1.0]>";
    }
    
    @Override
    public String getValidRange() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String setValidRange() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
