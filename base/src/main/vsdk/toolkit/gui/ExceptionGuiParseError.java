package vsdk.toolkit.gui;
import java.io.Serial;

public class ExceptionGuiParseError extends Exception {
    @Serial private static final long serialVersionUID = 20140314L;

    @Override
    public String toString(){
        return "Parse error reading GUI data";
    }
}
