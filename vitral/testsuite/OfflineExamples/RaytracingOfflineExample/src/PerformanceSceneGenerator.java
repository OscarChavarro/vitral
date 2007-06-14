//===========================================================================

import java.io.File;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;

public class PerformanceSceneGenerator
{
    private static void createScene(long numElements, BufferedOutputStream w)
    throws Exception
    {
        String header;

        header = "viewport 640 480\n" +
            "eye 0 -5 5\n" +
            "up 0 0 1\n" +
            "lookat 0 0 0\n" +
            "fov 45\n" +
            "background 0.078 0.361 0.753\n" +
            "surface 0.5 0.45 0.35 0.3 1.0 1 3 0 0.0 1.0\n" +
            "light 1 1 1 ambient\n" +
            "light 0.4 0.4 0.4 point 4 3 2\n";

        byte arr[];

        arr = header.getBytes();
        w.write(arr, 0, arr.length);

        double x, y, z, r;
        String elem;

        r = 0.1;
        y = 0;
        z = 0;
        x = -3.1;
        for ( long i = 0; i < numElements; i++ ) {
            x += 0.15;
            if ( x > 3 ) {
                x = -3.1;
                y += 0.3;
        }
            elem = "sphere " + x + " " + y + " " + z + " " + r + "\n";
            arr = elem.getBytes();
            w.write(arr, 0, arr.length);
    }
    }

    public static void main(String args[])
    {
        if ( args.length != 2 ) {
            System.err.println("Usage: specify:");
            System.err.println("<number of elements to create> <output file>\n");
            return;
    }

        long numElements = Long.parseLong(args[0]);

        System.out.println("Generating a test scene with " + numElements +
            " elements to file " + args[1]);

        try {
            File fd = new File(args[1]);

            BufferedOutputStream writer = new BufferedOutputStream(new FileOutputStream(fd));
            createScene(numElements, writer);
            writer.flush();
            writer.close();
    }
    catch ( Exception e ) {
            System.err.println("Cannot create file!");
    }

    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
