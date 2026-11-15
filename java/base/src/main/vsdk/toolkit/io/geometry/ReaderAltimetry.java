package vsdk.toolkit.io.geometry;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import vsdk.toolkit.common.VSDKFatalException;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.volume.HeightFieldSolid;

/**
Reads a text altimetry grid into a height field. The format is the header
"altimetry <columns> <rows>" followed by columns*rows heights, row by row
from z = 0, each row from x = 0; "#" starts a comment up to the end of the
line. Both counts must be at least 2. The solid's box spans the whole grid,
from the lowest to the highest height; when every height is equal the box
is one unit tall above them.
*/
public class ReaderAltimetry {
    private static final int EOF = -1;

    /** Byte stream with the single character push back of C `ungetc`. */
    private static class CharacterInput {
        private final InputStream source;
        private int pushed = EOF - 1;
        CharacterInput(InputStream source) { this.source = source; }
        int fgetc() throws IOException
        {
            if ( pushed != EOF - 1 ) { int c = pushed; pushed = EOF - 1; return c; }
            return source.read();
        }
        void ungetc(int c) { pushed = c; }
    }

    // Next whitespace separated token, skipping comments; null at end of file
    // (cpp: false). A token of `capacity` characters or more also gives null.
    private static String readToken(CharacterInput file, int capacity) throws IOException
    {
        int c = file.fgetc();
        while ( c != EOF ) {
            if ( c == '#' ) {
                while ( c != EOF && c != '\n' ) c = file.fgetc();
            }
            else if ( c == ' ' || c == '\t' || c == '\r' || c == '\n' ) {
                c = file.fgetc();
            }
            else {
                break;
            }
        }
        if ( c == EOF ) return null;
        StringBuilder token = new StringBuilder();
        while ( c != EOF && c != ' ' && c != '\t' && c != '\r' && c != '\n' && c != '#' ) {
            if ( token.length() + 1 >= capacity ) return null;
            token.append((char)c);
            c = file.fgetc();
        }
        if ( c == '#' ) file.ungetc(c);
        return token.toString();
    }

    /** `value` is a one-element output holder. */
    private static boolean parseNumber(String token, double[] value)
    {
        if ( token == null ) return false;
        Double parsed = _CScanf.strtodWhole(token);
        if ( parsed == null ) return false;
        value[0] = parsed;
        return Double.isFinite(value[0]);
    }

    // Throws VSDKFatalException for unreadable or malformed files.
    public static HeightFieldSolid importHeightField(String fileName) {
        InputStream stream;
        try {
            stream = new BufferedInputStream(new FileInputStream(fileName));
        }
        catch ( IOException e ) {
            throw new VSDKFatalException("ReaderAltimetry: could not open file");
        }
        try ( InputStream closing = stream ) {
            return importHeightField(new CharacterInput(closing));
        }
        catch ( IOException e ) {
            throw new VSDKFatalException("ReaderAltimetry: could not open file", e);
        }
    }

    private static HeightFieldSolid importHeightField(CharacterInput file) throws IOException {
        final int capacity = 128;
        String token;
        double[] columns = {0};
        double[] rows = {0};
        if ( (token = readToken(file, capacity)) == null || !token.equals("altimetry") ||
             !parseNumber(readToken(file, capacity), columns) ||
             !parseNumber(readToken(file, capacity), rows) ||
             columns[0] != (double)(long)columns[0] || rows[0] != (double)(long)rows[0] ||
             columns[0] < 2 || rows[0] < 2 || columns[0]*rows[0] > 1.0e8 ) {
            throw new VSDKFatalException("ReaderAltimetry: header must be \"altimetry <columns> <rows>\" with counts of at least 2");
        }
        final long columnCount = (long)columns[0];
        final long rowCount = (long)rows[0];
        HeightFieldSolid solid = new HeightFieldSolid(columnCount, rowCount,
            new Vector3Dd(0, 0, 0), new Vector3Dd((double)(columnCount - 1), 1, (double)(rowCount - 1)));
        double lowest = Double.POSITIVE_INFINITY;
        double highest = Double.NEGATIVE_INFINITY;
        for ( long z = 0; z < rowCount; z++ ) {
            for ( long x = 0; x < columnCount; x++ ) {
                double[] height = {0};
                if ( !parseNumber(readToken(file, capacity), height) ) {
                    throw new VSDKFatalException("ReaderAltimetry: missing or invalid height");
                }
                solid.setHeight(x, z, height[0]);
                if ( height[0] < lowest ) lowest = height[0];
                if ( height[0] > highest ) highest = height[0];
            }
        }
        final boolean extra = readToken(file, capacity) != null;
        if ( extra ) {
            throw new VSDKFatalException("ReaderAltimetry: more heights than columns*rows");
        }
        if ( !(highest > lowest) ) highest = lowest + 1.0;
        solid.setBounds(new Vector3Dd(0, lowest, 0),
            new Vector3Dd((double)(columnCount - 1), highest, (double)(rowCount - 1)));
        solid.updateBlocks();
        return solid;
    }
}
