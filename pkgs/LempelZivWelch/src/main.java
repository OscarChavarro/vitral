//===========================================================================

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import vsdk.toolkit.processing.LzwWrapper;

public class main
{
    public static void main(String args[])
    {
        FileInputStream fis;
        FileOutputStream fos;

        try {
/*
// THIS DOES NOT WORK YET!
            fis = new FileInputStream(new File("./src/main.java"));
            fos = new FileOutputStream(new File("./main.java.Z"));

  	    System.out.print("Compressing ./src/main.java on main.java.Z: ");
            LzwWrapper.compress(fis, fos);
	    System.out.println("Ok!");
*/
            fis = new FileInputStream(new File("./main.java.Z"));
            fos = new FileOutputStream(new File("./main.java"));

  	    System.out.print("Uncompressing ./main.java.Z on main.java: ");
            LzwWrapper.decompress(fis, fos);
	    System.out.println("Ok!");
	}
	catch ( Exception e ) {
	    e.printStackTrace();
	}
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
