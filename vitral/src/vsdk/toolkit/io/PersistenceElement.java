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
    protected void
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
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
