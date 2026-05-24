package vsdk.toolkit.io.geometry.stepCad.writer;

import static vsdk.toolkit.io.geometry.stepCad.writer._StepEntityBuffer.escape;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import vsdk.toolkit.io.PersistenceElement;

/**
Writes the fixed ISO 10303-21 wrapper of a STEP file: the leading
ISO-10303-21 / HEADER section with FILE_DESCRIPTION, FILE_NAME and
FILE_SCHEMA, the DATA / ENDSEC delimiters, and the closing
END-ISO-10303-21 sentinel.

This is an internal collaborator of `StepWriter`.
*/
public class _StepHeaderWriter extends PersistenceElement {

    public static final String SCHEMA_AP242 =
        "AP242_MANAGED_MODEL_BASED_3D_ENGINEERING_MIM_LF{1 0 10303 442 3 1 4}";

    private _StepHeaderWriter()
    {
    }

    public static void writeHeader(OutputStream stream, String productName)
        throws Exception
    {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
        String timestamp = fmt.format(new Date());

        writeAsciiLine(stream, "ISO-10303-21;");
        writeAsciiLine(stream, "HEADER;");
        writeAsciiLine(stream,
            "FILE_DESCRIPTION(('Polyhedral B-Rep exported from VITRAL kernel'),'2;1');");
        writeAsciiLine(stream,
            "FILE_NAME('" + escape(productName) + "','" + timestamp
            + "',('VITRAL'),('VITRAL'),'VITRAL stepCad writer','VITRAL','');");
        writeAsciiLine(stream,
            "FILE_SCHEMA(('" + SCHEMA_AP242 + "'));");
        writeAsciiLine(stream, "ENDSEC;");
    }

    public static void writeDataSectionOpen(OutputStream stream) throws Exception
    {
        writeAsciiLine(stream, "DATA;");
    }

    public static void writeDataSectionClose(OutputStream stream) throws Exception
    {
        writeAsciiLine(stream, "ENDSEC;");
        writeAsciiLine(stream, "END-ISO-10303-21;");
    }
}
