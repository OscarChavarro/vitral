package vsdk.toolkit.gui.widget.variable;

public class WidgetIntegerVariable extends WidgetVariable {
    @Override
    public String getType() {
        return "Integer";
    }

    public WidgetIntegerVariable() {
        super();

        this.validRange = "(-INF, INF)";
    }

    @Override
    public String getValidRange() {
        return validRange;
    }

    public String setValidRange() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setValidRange(String vr) {
        this.validRange = vr;
    }

}
