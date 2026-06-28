package vsdk.toolkit.gui.widget.variable;

public class WidgetVector3DVariable extends WidgetVariable {
    @Override
    public String getType() {
        return "Vector3Dd";
    }

    public WidgetVector3DVariable() {
        super();
        this.validRange = "<(-INF, INF), (-INF, INF), (-INF, INF)>";
    }

    @Override
    public String getValidRange() {
        return this.validRange;
    }

    public void setValidRange() {

    }

    @Override
    public void setValidRange(String vr) {
        this.validRange = vr;
    }
}
