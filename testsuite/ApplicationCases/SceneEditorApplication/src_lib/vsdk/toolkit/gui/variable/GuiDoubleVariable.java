/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vsdk.toolkit.gui.variable;

/**
 *
 * @author TaakeSlottet
 */
public class GuiDoubleVariable extends GuiVariable {
    @Override
    public String getType() {
        return "Double";
    }
    
    public GuiDoubleVariable() {
        super();
        this.validRange = "(-INF, INF)";
    }

    @Override
    public String getValidRange() {
        return validRange;
    }

    public void setValidRange(String vr) {
        validRange = vr;
    }


    public String setValidRange() {
        return "Raro";
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
