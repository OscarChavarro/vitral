package vsdk.toolkit.io.geometry.stepCad.writer;

import java.io.OutputStream;
import java.util.List;
import java.util.Locale;

/**
Accumulator for ISO 10303-21 entity instances. Owns the monotonically
growing entity-id counter and the buffered DATA section text. Also
provides the shared formatting utilities used by all other emitters in
this module.

Although `public`, this class is an internal collaborator of `StepWriter`
(hence the leading underscore in the file name). External callers should
use `StepWriter`.
*/
public class _StepEntityBuffer {

    private final StringBuilder dataSection;
    private int nextEntityId;

    public _StepEntityBuffer()
    {
        this.dataSection = new StringBuilder();
        this.nextEntityId = 1;
    }

    public int nextId()
    {
        int id = nextEntityId;
        nextEntityId++;
        return id;
    }

    public void appendEntity(int id, String body)
    {
        dataSection.append('#').append(id).append('=').append(body)
            .append(';').append('\n');
    }

    public void writeTo(OutputStream stream) throws Exception
    {
        byte[] data = dataSection.toString().getBytes();
        stream.write(data, 0, data.length);
    }

    //=================================================================

    public static String refList(List<Integer> ids)
    {
        StringBuilder sb = new StringBuilder();
        int i;
        for ( i = 0; i < ids.size(); i++ ) {
            if ( i > 0 ) {
                sb.append(',');
            }
            sb.append('#').append(ids.get(i));
        }
        return sb.toString();
    }

    public static String fmt(double value)
    {
        if ( value == 0.0 ) {
            return "0.0";
        }
        return String.format(Locale.US, "%.15E", value);
    }

    public static String escape(String text)
    {
        if ( text == null ) {
            return "";
        }
        return text.replace("\\", "\\\\").replace("'", "''");
    }
}
