package vsdk.toolkit.gui.widget.variable;

public class WidgetStringVariable extends WidgetVariable {
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
