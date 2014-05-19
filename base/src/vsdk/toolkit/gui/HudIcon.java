//===========================================================================
package vsdk.toolkit.gui;

import java.io.File;
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.gui.KeyEvent;
import vsdk.toolkit.io.image.ImagePersistence;
import vsdk.toolkit.media.Image;
import vsdk.toolkit.media.RGBAImage;
import vsdk.toolkit.media.RGBAPixel;
import vsdk.toolkit.media.RGBPixel;

/**
Class used to manage HUD based interactions based on button like images with
interaction area.
*/
public class HudIcon extends GuiElement {
    /// Image to show, can be null
    private RGBAImage image;   
    
    /// Image coordinate to locate inside framebuffer. Can be negative following
    /// X11 geometry style
    private int x;
    private int y;

    /// Can be 0, in that case size is taken from image size
    private int xSize;
    private int ySize;
    
    /// URL to link when user does it click
    private int keyEvent;

    public HudIcon(int x, int y, int xSize, int ySize, String imageFilename, int keyEvent)
    {
        init(x, y, xSize, ySize, keyEvent, imageFilename);
    }

    /**
    Generates a segment of HTML1 code to encode current icon over a map for
    simple image interaction.
    @param baseUrl
    @return a string representing contents 
    */
    public String getMapTag(String baseUrl)
    {
        return "    <AREA SHAPE=\"rect\" COORDS=\"" + 
                x + ", " + 
                y + ", " +
                (x + xSize) + ", " + 
                (y + ySize) + "\" HREF=\"" + 
                baseUrl + KeyEvent.getKeyName(keyEvent) + "\" " +
                "style=\"cursor: default;\"" + 
                "/>";
    }
    
    /**
    Modifies output image to add current image over it in the corresponding
    coordinates
    @param output Image to where pixels will be drawn
    */
    public void overWritePixels(Image output)
    {

        if ( image == null ) {
            return;
        }

        int myXSize = image.getXSize();
        int myYSize = image.getYSize();
        int xi, yi;
        RGBAPixel origin = new RGBAPixel();
        RGBPixel target = new RGBPixel();

        for ( xi = 0; xi < myXSize; xi++ ) {
            for ( yi = 0; yi < myYSize; yi++ ) {
                image.getPixelRgba(xi, yi, origin);
                target.r = origin.r;
                target.g = origin.g;
                target.b = origin.b;
                if ( VSDK.signedByte2unsignedInteger(origin.a) > 250 ) {
                    output.putPixelRgb(xi + x, yi + y, target);
                }
            }
        }
        
    }
    
    private void init(int x, int y, int xSize, int ySize, int keyEvent, String imageFilename) {
        this.x = x;
        this.y = y;
        this.xSize = xSize;
        this.ySize = ySize;
        this.keyEvent = keyEvent;
        
        try {
            image = null;
            
            if ( imageFilename != null ) {
                image = ImagePersistence.importRGBA(new File(imageFilename));
                if ( xSize == 0 ) {
                    this.xSize = image.getXSize();
                }
                if ( ySize == 0 ) {
                    this.ySize = image.getYSize();
                }
            }
            
        }
        catch (Exception e) {
            VSDK.reportMessageWithException(this, VSDK.WARNING, "WebIcon", "Image not found", e);
        }
    }
    
    /**
    @return the image
    */
    public RGBAImage getImage() {
        return image;
    }

    /**
    @param image the image to set
    */
    public void setImage(RGBAImage image) {
        this.image = image;
    }
    
    @Override
    public String toString()
    {
        String msg;
        
        if ( image != null ) { 
            msg = "ICON -> x:" + x + " y: " + y + " xSize: " + 
                    xSize + " ySize: " + ySize +
                " img: LOADED keyEvent: " + KeyEvent.getKeyName(keyEvent) + "\n";
        }
        else {
            msg = "ICON -> x:" + x + " y: " + y + " xSize: " + 
                    xSize + " ySize: " + ySize +
                " img: null keyEvent: " + KeyEvent.getKeyName(keyEvent) + "\n";
        }
        
        return msg;
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
