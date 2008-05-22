//===========================================================================

import java.io.File;
import java.io.FileInputStream;

public class Configuration
{
    public int windowXSize;
    public int windowYSize;
    public int numberOfFrames;
    boolean withDisplayList;
    boolean withVertexArrays;
    boolean withBackFaceCulling;
    public String dataset;
    public FileInputStream fisGeometry;
    public FileInputStream fisColors;

    public Configuration()
    {
        windowXSize = 640;
        windowYSize = 480;
        dataset = null;
        fisGeometry = null;
        fisColors = null;
        withDisplayList = false;
        numberOfFrames = -1;
        withVertexArrays = false;
    }

    private void openDataset()
    {
        String name1 = dataset + ".bin";
        String name2 = dataset + ".clr";

        try {
            fisGeometry = new FileInputStream(new File(name1));
            fisColors = new FileInputStream(new File(name2));
        }
        catch ( Exception e ) {
            System.err.println("Error opening dataset " + dataset + ".");
            e.printStackTrace();
            System.exit(0);
        }
    }

    public void process(String args[])
    {
        int i;
        String ptr;

        if ( args.length < 1 ) {
            System.err.println("Usage:\n\tjava QuadBenchmark <options>");
            System.exit(1);
        }

        for ( i = 0; i < args.length; i++ ) {
            if ( args[i].equals("-qd") ) {
                dataset = args[i+1]; 
                openDataset();
                i++;
            }
            else if ( args[i].equals("-dl") ) {
                withDisplayList = true;
            }
            else if ( args[i].equals("-va") ) {
                withVertexArrays = true;
            }
            else if ( args[i].equals("-bf") ) {
                withBackFaceCulling = true;
            }
            else if ( args[i].equals("-nf") ) {
                numberOfFrames = Integer.parseInt(args[i+1]);
                i++;
            }
            else if ( args[i].equals("-xws") ) {
                windowXSize = Integer.parseInt(args[i+1]);
                i++;
            }
            else if ( args[i].equals("-yws") ) {
                windowYSize = Integer.parseInt(args[i+1]);
                i++;
            }
            else {
                System.err.println("Unknown option " + args[i]);
                System.exit(0);
            }
        }
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
