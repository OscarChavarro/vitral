package vsdk.toolkit.gui;
import java.io.Serial;

public class ExceptionGuiBadName extends Exception {
    @Serial private static final long serialVersionUID = 20140314L;

    @Override
    public String toString(){
        return "Bad name";
    }
}
