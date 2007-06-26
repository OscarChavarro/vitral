//===========================================================================

// Java basic classes
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

// Java Awt + Swing + Applet classes
import java.awt.Dimension;
import java.awt.Image;
import java.awt.image.PixelGrabber;
import java.applet.Applet;

public class Submit
{
    static boolean connected = false;
    static DataOutputStream printout;
    static URLConnection urlConn;
    
    public boolean connect(Applet applet) {
        try {
            String pageUrl = Globals.BASE_URL_SKETCH;
            System.out.println("Url for sketch reporting: " + pageUrl);
            URL url = new URL(pageUrl);
            urlConn = url.openConnection();
            urlConn.setDoInput(true);
            urlConn.setDoOutput(true);
            urlConn.setUseCaches(false);
            urlConn.setRequestProperty("Content-Type",
                                       "application/x-www-form-urlencoded");
            printout = new DataOutputStream(urlConn.getOutputStream());
          }
          catch ( Exception exception ) {
            System.out.println(exception);
            return false;
        }
        connected = true;
        return true;
    }

    /**
    Given an image (inside the `sketchcanvas`), this method creates a String
    for it in the following format:
        ImageXSize\nImageYSize\nIndexValuesSequence
    where IndexValuesSequence is a sequence of numbers separated by a space
    (' ') sign. Each index in the sequence identifies a black pixel in the
    image.  Note this method treats the image as a binary one.
    */
    private static String createPixelString(SketchCanvas sketchcanvas) {
        //-----------------------------------------------------------------
        Dimension dimension = sketchcanvas.getSize();
        int pixelArray[];

        pixelArray = getPixels(sketchcanvas.get_image(),
                               0, 0, dimension.width, dimension.height);
        if ( pixelArray == null ) {
            return null;
        }

        //-----------------------------------------------------------------
        int i = 0;
        int pixelValue;
        int acumNumberOfPixels = 0;
        StringBuffer stringbuffer = new StringBuffer();
        stringbuffer.append(Integer.toString(dimension.width) + "\n");
        stringbuffer.append(Integer.toString(dimension.height) + "\n");
        int index = 0;
        int x, y;
        for ( y = 0; y < dimension.height; y++ ) {
            for ( x = 0; x < dimension.width; x++, index++ ) {
                pixelValue = pixelArray[index] & 0xff;
                if ( pixelValue == 0 ) {
                    acumNumberOfPixels++;
                    stringbuffer.append(Integer.toString(index) + " ");
                  }
                  else {
                    i++;
                }
            }
        }
        System.out.print("(" + acumNumberOfPixels + " pixels) ");
        return stringbuffer.toString();
    }
    
    private static int[]
    getPixels(Image image, int startX, int startY, int width, int height) {
        int[] pixelArray = new int[width * height];
        PixelGrabber pixelgrabber =
          new PixelGrabber(image, startX, startY, width, height,
                           pixelArray, 0, width);
        boolean bool;
        try {
            bool = pixelgrabber.grabPixels();
          }
          catch ( Exception exception ) {
            System.out.println("Error grabbing pixels: " + exception);
            return null;
        }
        if ( (pixelgrabber.getStatus() & 0x80) != 0 ) {
            System.out.println("Error grabbing pixels");
            return null;
        }
        return pixelArray;
    }
    
    public static void receive(String[] strings) {
        int i = 0;
        System.out.println("Submit::receive(): ... ");
        try {
            java.io.InputStream inputstream = urlConn.getInputStream();
            BufferedReader br
                = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
            String string;
            do {
                string = br.readLine();
                System.out.println("rec: [" + string + "]");
                if ( string != null && string.startsWith("filespec:") ) {
                    strings[i] = new String(string.substring(10));
                    i++;
                }
            } while ( (string != null) && !string.startsWith("done") );
            br.close();
            System.out.println("Read " + i + " filespecs.");
          }
          catch (Exception exception) {
            System.out.println("Error receiving: " + exception);
        }
    }

    public static void
    send(SketchCanvas sketchcanvases[], int numberOfImages) {
        int i;
        String urlGetParametersString;

        try {
            if ( connected ) {
                urlGetParametersString = "nr_sketches=" +
                  URLEncoder.encode(Integer.toString(numberOfImages), "UTF-8");
                for ( i = 0; i < numberOfImages; i++ ) {
                    if ( sketchcanvases[i].is_empty() ) {
                        System.out.println(
                            "Image " + (i + 1) + "/" + numberOfImages + " is empty, skipping.");
                    }
                    else {
                        System.out.print("Image " + (i + 1) + "/" + numberOfImages + ": creating pixel string... ");
                        String pixelString = createPixelString(sketchcanvases[i]);
                        urlGetParametersString += ("&image" + (i + 1) + "="
                                   + URLEncoder.encode(pixelString, "UTF-8"));
                        urlGetParametersString += ("&history" + (i + 1) + "="
                                   + URLEncoder.encode(sketchcanvases[i]
                                                       .get_history_text(), "UTF-8"));
                        System.out.println("Ok.");
                    }
                }
                System.out.print("Sending pixel strings... ");
                printout.writeBytes(urlGetParametersString);
                printout.flush();
                printout.close();
                System.out.println("Ok!");
                //System.out.println("Query URL: " + urlGetParametersString);
                connected = false;
              }
              else {
                System.out.println("Error: no connection, cannot send images.");
            }
        }
        catch ( Exception e ) {
            System.err.println("ERROR Encoding URL in Sumbit.send();");
            System.err.println(e);
            connected = false;
        }
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
