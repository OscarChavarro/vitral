//===========================================================================
import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.BufferedInputStream;

import vsdk.toolkit.io.PersistenceElement;

/**
This program reads a text file with AQUYNZA/VITRAL standard format for
activities log, and reports totals and format failures.
*/
public class LogAnalyzer
{
    public static final int NEW_DAY_LINE = 1;
    public static final int INIT_ACTIVITY_LINE = 2;
    public static final int CONTINUE_ACTIVITY_LINE = 3;
    public static final int EMPTY_LINE = 4;
    public static final int INVALID_LINE = 5;

    private static boolean checkForSeparator(String line)
    {
        int i;
        for ( i = 0; i < line.length(); i++ ) {
            char c = line.charAt(i);

            if ( !(c == '=' || c == '-' || Character.isSpaceChar(c) ) ) {
                return false;
            }
        }
        return true;
    }

    private static int classifyLine(String line)
    {
        int type = INVALID_LINE;
        int i;
        boolean firstTimer = true;
        boolean withSpaces = false;

        for ( i = 0; i < line.length(); i++ ) {
            char c = line.charAt(i);
            if ( Character.isSpaceChar(c) ) {
                withSpaces = true;
                continue;
            }
            if ( firstTimer && !withSpaces && Character.isDigit(c) ) {
                return NEW_DAY_LINE;
            }
            else if ( firstTimer && withSpaces && Character.isDigit(c) ) {
                return INVALID_LINE;
            }
            else if ( firstTimer && c == '-' ) { 
                if ( checkForSeparator(line) ) {
                    return EMPTY_LINE;
                }
                else {
                    return INIT_ACTIVITY_LINE;
                }
            }
            else if ( firstTimer && c == '=' ) { 
                return EMPTY_LINE;
            }
            else if ( firstTimer && Character.isLetter(c) ) {
                return CONTINUE_ACTIVITY_LINE;
            }
        }

        return type;
    }


    private static void processFile(InputStream is) throws Exception
    {
        String line;
        int lineNumber;

        for ( lineNumber = 1; is.available() > 0; lineNumber++ ) {
            line = PersistenceElement.readAsciiLine(is);
            if ( line == null || line.length() <= 0 ) {
                continue;
            }

            int lineType;

            lineType = classifyLine(line);

            if ( lineType == INVALID_LINE ) {
                System.err.println("ERROR: At line " + lineNumber + ": line cannot start with numeric values");
                System.out.println(line);
            }

            switch ( lineType ) {
              case NEW_DAY_LINE:
                System.out.print("[NEW_DAY]: ");
                break;
              case INIT_ACTIVITY_LINE:
                System.out.print("[INIT_ACTIVITY]: ");
                break;
              case CONTINUE_ACTIVITY_LINE:
                System.out.print("[CONTINUE_ACTIVITY]: ");
                break;
              case EMPTY_LINE:
                continue;
              default:
                System.out.print("[*INVALID*]: ");
                break;
            }

            System.out.println(line);
        }
    }

    public static void main(String args[])
    {
        if ( args.length == 0 ) {
            System.err.println("Usage:");
            System.err.println("    java LogAnalyzer logfile.txt ...");
        }

        int i;
        File fd;
        FileInputStream fis;
        BufferedInputStream bis;

        for ( i = 0; i < args.length; i++ ) {
            try {
                fd = new File(args[i]);
                fis = new FileInputStream(fd);
                bis = new BufferedInputStream(fis);
                processFile(bis);
            }
            catch ( Exception e ) {
                e.printStackTrace();
            }
        }
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
