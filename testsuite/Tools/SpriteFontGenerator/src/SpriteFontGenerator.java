// Java classes
import java.io.File;
import java.awt.Font;

// VSDK classes
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.gui.AwtSystem;
import vsdk.toolkit.media.RGBAImageUncompressed;
import vsdk.toolkit.io.image.ImagePersistence;

/**
Given a font and a font size, this program generates a set of transparent
bitmap for sprites corresponding to image versions of several characters,
rendered from the given font.
*/
public class SpriteFontGenerator
{
    private static void doGlyph(char c)
    {
        RGBAImageUncompressed img;
        Font font;
        String s = "" + c;
	System.out.print(s);
        img = new RGBAImageUncompressed();
        img.init(320, 200);
        img.createTestPattern();
        font = new Font("Arial", Font.PLAIN, 50);
        img = AwtSystem.calculateLabelImage(s, new ColorRgb(1.0, 1.0, 1.0), font);
        if ( s.equals(".") ) {
            ImagePersistence.exportPNG(new File("./output/dot.png"), img);
	  }
  	  else {
            ImagePersistence.exportPNG(new File("./output/" + s + ".png"), img);
	}
    }

    public static void main(String args[]) {
        char c;
	System.out.print("Generating sprite images for following characters:\n  ");
	for ( c = 'A'; c < 'Z'; c++ ) {
	    doGlyph(c);
	}
	System.out.print("\n  ");
	for ( c = 'a'; c < 'z'; c++ ) {
	    doGlyph(c);
	}
	System.out.print("\n  ");
	for ( c = '0'; c < '9'; c++ ) {
	    doGlyph(c);
	}
        doGlyph('-');
        doGlyph('_');
        doGlyph('.');
        doGlyph('á');
        doGlyph('é');
        doGlyph('í');
        doGlyph('ó');
        doGlyph('ú');
        doGlyph('Á');
        doGlyph('É');
        doGlyph('Í');
        doGlyph('Ó');
        doGlyph('Ú');
        doGlyph('Ñ');
        doGlyph('ñ');
	System.out.println("");
    }
}
