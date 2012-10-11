/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vsdk.toolkit.gui.variable;

/**
 *
 * @author TaakeSlottet
 */
public class GuiIntegerVariable extends GuiVariable {
    @Override
    public String getType() {
        return "Integer";
    }

    public GuiIntegerVariable() {
        super();
                
        this.validRange = "(-INF, INF)";
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
