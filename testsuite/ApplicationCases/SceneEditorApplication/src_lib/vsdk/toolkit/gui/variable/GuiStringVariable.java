/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vsdk.toolkit.gui.variable;

/**
 *
 * @author TaakeSlottet
 */
public class GuiStringVariable extends GuiVariable {
    @Override
    public String getType() {
        return "String";
    }

    @Override
    public String getValidRange() {
        return  validRange;
    }

    public String setValidRange() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setValidRange(String vr) {
        validRange = vr;
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
