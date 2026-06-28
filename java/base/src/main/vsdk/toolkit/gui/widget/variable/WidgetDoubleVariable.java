package vsdk.toolkit.gui.widget.variable;

public class WidgetDoubleVariable extends WidgetVariable {
    @Override
    public String getType() {
        return "Double";
    }

    public WidgetDoubleVariable() {
        super();
        this.validRange = "(-INF, INF)";
    }

    @Override
    public String getValidRange() {
        return validRange;
    }

    @Override
    public void setValidRange(String vr) {
        validRange = vr;
    }


    public String setValidRange() {
        return "Raro";
    }
}
