package vsdk.toolkit.render.android;

// Android classes: misc
import android.graphics.Bitmap;
import android.graphics.Color;

// VitralSDK classes
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.render.RenderingElement;
import vsdk.toolkit.media.RGBAPixel;
import vsdk.toolkit.media.RGBAImageUncompressed;

public class AndroidRGBAImageUncompressedRenderer extends AndroidRenderer {
    public static boolean
    importFromAndroidBitmap(Bitmap input, RGBAImageUncompressed output)
    {
        output.init(input.getWidth(), input.getHeight());

        //input.copyPixelsToBuffer(output.getRawImageDirectBuffer());
        int x;
        int y;
        RGBAPixel p = new RGBAPixel();
        int c;

        for ( y = 0; y < output.getYSize(); y++ ) {
            for ( x = 0; x < output.getXSize(); x++ ) {
                c = input.getPixel(x, y);
                p.r = VSDK.unsigned8BitInteger2signedByte(Color.red(c));
                p.g = VSDK.unsigned8BitInteger2signedByte(Color.green(c));
                p.b = VSDK.unsigned8BitInteger2signedByte(Color.blue(c));
                p.a = VSDK.unsigned8BitInteger2signedByte(Color.alpha(c));
                output.putPixel(x, y, p);
            }
        }

        return true;
    }
}
