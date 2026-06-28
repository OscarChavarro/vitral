package vsdk.toolkit.gui.widget.variable;

public class WidgetColorRgbVariable extends WidgetVariable {

    @Override
    public String getType() {
        return "ColorRgb";
    }

    public WidgetColorRgbVariable() {
        super();
        this.validRange = "<[0.0, 1.0], [0.0, 1.0], [0.0, 1.0]>";
    }

    @Override
    public String getValidRange() {
        return this.validRange;
    }

    public String setValidRange() {
        return validRange;
    }

    @Override
    public void setValidRange(String vr) {
        this.validRange = vr;
    }

}
