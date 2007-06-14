//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - December 8 2006 - Oscar Chavarro: Original base version               =
//===========================================================================

package vsdk.toolkit.io;

import java.io.File;
import java.io.InputStream;
import java.util.StringTokenizer;

/**
DEFINITION: A `PersistenceElement` in VitralSDK is a software element with
algorithms and data structures (i.e. a class) with the specific functionality
of providing persistence operations for a data Entity.

The PersistenceElement abstract class provides an interface for *Persistence
style classes. This serves two purposes:
  - To help in design level organization of persistence classes (this eases the
    study of the class hierarchy)
  - To provide a place to locate possible future operations, common to
    all persistence classes and persistence private utility/supporting
    classes.  In particular, this class contains basic low level
    persistence operations for converting bit streams from and to basic
    numeric data types. Note that this code is NOT portable, as it needs
    explicit programmer configuration for little-endian or big-endian
    hardware platform.
*/

public abstract class PersistenceElement {

    private static final boolean bigEndianArchitecture = false;

    private static byte[] bytesForInt = new byte[2];
    private static byte[] bytesForLong = new byte[4];
    private static byte[] bytesForFloat = new byte[4];

    /**
    Given a filename, this method extract its extension and return it.
    @todo: This method will fail when directory path or filename contains
    more than one dot.  Needs to be fixed.
    */
    protected static String extractExtensionFromFile(File fd)
    {
        String filename = fd.getName();
        StringTokenizer st = new StringTokenizer(filename, ".");
        int numTokens = st.countTokens();
        for( int i = 0; i < numTokens - 1; i++ ) {
            st.nextToken();
        }
        String ext = st.nextToken();
        return ext;
    }

    /**
    Given a previously initialized array of bytes, this method fills it
    with information readed from the given input stream.  If it is not
    enough information to read, this method generates an Exception.
    */
    protected static void
    readBytes(InputStream is, byte[] bytesBuffer) throws Exception
    {
        int offset = 0;
        int numRead = 0;
        int length = bytesBuffer.length;
        do {
            numRead = is.read(bytesBuffer, 
                              offset, (int)(length-offset));
            offset += numRead;
        } while( offset < length && numRead >= 0 ); 
    }

    private static int byteArray2intDirect(byte[] arr, int start) {
    int low = arr[start] & 0xff;
    int high = arr[start+1] & 0xff;
    return (int)( high << 8 | low );
    }

    private static int byteArray2intInvert(byte[] arr, int start) {
    int low = arr[start] & 0xff;
    int high = arr[start+1] & 0xff;
    return (int)( low << 8 | high );
    }

    private static long byteArray2longDirect(byte[] arr, int start) {
    int i = 0;
    int len = 4;
    int cnt = 0;
    byte[] tmp = new byte[len];
    for ( i = start; i < (start + len); i++ ) {
        tmp[cnt] = arr[i];
        cnt++;
    }
    long accum = 0;
    i = 0;
    for ( int shiftBy = 0; shiftBy < 32; shiftBy += 8 ) {
        accum |= ( (long)( tmp[i] & 0xff ) ) << shiftBy;
        i++;
    }
    return accum;
    }

    private static long byteArray2longInvert(byte[] arr, int start) {
    int i = 0;
    int len = 4;
    int cnt = 3;
    byte[] tmp = new byte[len];
    for ( i = start; i < (start + len); i++ ) {
        tmp[cnt] = arr[i];
        cnt--;
    }
    long accum = 0;
    i = 0;
    for ( int shiftBy = 0; shiftBy < 32; shiftBy += 8 ) {
        accum |= ( (long)( tmp[i] & 0xff ) ) << shiftBy;
        i++;
    }
    return accum;
    }

    private static float byteArray2floatDirect(byte[] arr, int start) {
    int i = 0;
    int len = 4;
    int cnt;
    byte[] tmp = new byte[len];

    for ( i = start, cnt = 0; i < (start + len); i++, cnt++ ) {
        tmp[cnt] = arr[i];
    }
    int accum = 0;
    i = 0;
    for ( int shiftBy = 0; shiftBy < 32; shiftBy += 8 ) {
        accum |= ( (long)( tmp[i] & 0xff ) ) << shiftBy;
        i++;
    }
    return Float.intBitsToFloat(accum);
    }

    private static float byteArray2floatInvert(byte[] arr, int start) {
    int i = 0;
    int len = 4;
    int cnt = 3;
    byte[] tmp = new byte[len];
    for ( i = start; i < (start + len); i++ ) {
        tmp[cnt] = arr[i];
        cnt--;
    }
    int accum = 0;
    i = 0;
    for ( int shiftBy = 0; shiftBy < 32; shiftBy += 8 ) {
        accum |= ( (long)( tmp[i] & 0xff ) ) << shiftBy;
        i++;
    }
    return Float.intBitsToFloat(accum);
    }

    /**
    This method is responsible of taking into account the endianess of the 
    original data
    */
    public static int byteArray2intBE(byte[] arr, int start) {
        if ( bigEndianArchitecture ) {
        return byteArray2intDirect(arr, start);
    }
    return byteArray2intInvert(arr, start);
    }

    /**
    This method is responsible of taking into account the endianess of the 
    original data
    */
    public static int byteArray2intLE(byte[] arr, int start) {
        if ( bigEndianArchitecture ) {
        return byteArray2intInvert(arr, start);
    }
    return byteArray2intDirect(arr, start);
    }


    /**
    This method is responsible of taking into account the endianess of the 
    original data
    */
    public static long byteArray2longBE(byte[] arr, int start) {
        if ( bigEndianArchitecture ) {
        return byteArray2longDirect(arr, start);
    }
    return byteArray2longInvert(arr, start);
    }

    /**
    This method is responsible of taking into account the endianess of the 
    original data
    */
    public static long byteArray2longLE(byte[] arr, int start) {
        if ( bigEndianArchitecture ) {
        return byteArray2longInvert(arr, start);
    }
    return byteArray2longDirect(arr, start);
    }

    /**
    This method is responsible of taking into account the endianess of the 
    original data
    */
    public static float byteArray2floatBE(byte[] arr, int start) {
        if ( bigEndianArchitecture ) {
        return byteArray2longDirect(arr, start);
    }
    return byteArray2longInvert(arr, start);
    }

    /**
    This method is responsible of taking into account the endianess of the 
    original data
    */
    public static float byteArray2floatLE(byte[] arr, int start) {
        if ( bigEndianArchitecture ) {
        return byteArray2floatInvert(arr, start);
    }
    return byteArray2floatDirect(arr, start);
    }

    protected static int readIntLE(InputStream is) throws Exception
    {
        readBytes(is, bytesForInt);
        return byteArray2intLE(bytesForInt, 0);
    }

    protected static int readIntBE(InputStream is) throws Exception
    {
        readBytes(is, bytesForInt);
        return byteArray2intBE(bytesForInt, 0);
    }

    protected static long readLongLE(InputStream is) throws Exception
    {
        readBytes(is, bytesForLong);
        return byteArray2longLE(bytesForLong, 0);
    }

    protected static long readLongBE(InputStream is) throws Exception
    {
        readBytes(is, bytesForLong);
        return byteArray2longLE(bytesForLong, 0);
    }
    protected static float readFloatLE(InputStream is) throws Exception
    {
        readBytes(is, bytesForFloat);
        return byteArray2floatLE(bytesForFloat, 0);
    }

    protected static float readFloatBE(InputStream is) throws Exception
    {
        readBytes(is, bytesForFloat);
        return byteArray2floatBE(bytesForFloat, 0);
    }

    protected static String readAsciiString(InputStream is) throws Exception
    {
        byte character[] = new byte[1];
        char letter;
        String msg = "";

        do {
            readBytes(is, character);
            letter = (char)character[0];
            if ( character[0] != 0x00 ) {
                msg = msg + letter;
        }
        } while ( character[0] != 0x00 );
        return msg;
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
