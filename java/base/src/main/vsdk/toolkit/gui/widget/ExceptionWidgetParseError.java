package vsdk.toolkit.gui.widget;
import java.io.Serial;

public class ExceptionWidgetParseError extends Exception {
    @Serial private static final long serialVersionUID = 20140314L;

    @Override
    public String toString(){
        return "Parse error reading GUI data";
    }
}
