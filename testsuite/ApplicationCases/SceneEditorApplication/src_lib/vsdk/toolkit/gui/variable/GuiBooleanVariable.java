/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vsdk.toolkit.gui.variable;

/**
 *
 * @author TaakeSlottet
 */
public class GuiBooleanVariable extends GuiVariable {

    public GuiBooleanVariable() {
        this.validRange = "false, true";
    }
    
    @Override
    public String getType() {
        return "Boolean";
    }

    @Override
    public String getValidRange() {
        return validRange;
    }

    @Override
    public String setValidRange() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
