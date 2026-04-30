package vsdk.toolkit.io.image;

// Java classes
import java.io.File;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.IOException;

// Android classes: misc
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;

// VitralSDK classes
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.media.RGBImageUncompressed;
import vsdk.toolkit.media.RGBAImageUncompressed;
import vsdk.toolkit.media.RGBPixel;
import vsdk.toolkit.render.android.AndroidRGBImageUncompressedRenderer;
import vsdk.toolkit.render.android.AndroidRGBAImageUncompressedRenderer;

public class ImagePersistenceAndroid extends ImagePersistenceHelper
{
    public boolean rgbFormatFromInputStreamSupported(String fileExtension)
    {
        return true;
    }

    public boolean rgbaFormatFromInputStreamSupported(String fileExtension)
    {
        return true;
    }

    public boolean rgbaFormatSupported(String fileExtension)
    {
        if ( fileExtension.equals("png") || fileExtension.equals("jpg") ) {
            return true;
        }
        return false;
    }

    public boolean rgbFormatSupported(String fileExtension)
    {
        if ( fileExtension.equals("png") || fileExtension.equals("jpg") ) {
            return true;
        }
        return false;
    }

    public RGBImageUncompressed importRGB(InputStream is) throws ImageNotRecognizedException, Exception
    {
        RGBImageUncompressed img;
        img = new RGBImageUncompressed();

        Bitmap bitmap = null;
        try {
            bitmap = BitmapFactory.decodeStream(is);
            is.close();
          } 
          catch(IOException e) {
        }

        img.init(bitmap.getWidth(), bitmap.getHeight());

        AndroidRGBImageUncompressedRenderer.importFromAndroidBitmap(bitmap, img);

        bitmap.recycle();

        return img;
    }

    public RGBAImageUncompressed importRGBA(File inImageFd) throws ImageNotRecognizedException, Exception
    {
        RGBAImageUncompressed img;

        if ( !inImageFd.exists() ) {
            VSDK.reportMessage(this, VSDK.WARNING,
                "importRGBA", "ERROR: file +\"" + 
			       inImageFd.getPath() + "\" does not exist!");
            img = new RGBAImageUncompressed();
            img.init(32, 32);
            img.createTestPattern();
            return img;
        }

        FileInputStream fis = new FileInputStream(inImageFd);
        BufferedInputStream bis = new BufferedInputStream(fis);

        img = importRGBA(bis);

        bis.close();
        return img;
    }

    public RGBImageUncompressed importRGB(File inImageFd) throws ImageNotRecognizedException, Exception
    {
        RGBImageUncompressed img;

        if ( !inImageFd.exists() ) {
            VSDK.reportMessage(this, VSDK.WARNING,
                "importRGBA", "ERROR: file +\"" + 
                inImageFd.getPath() + "\" does not exist!");
            img = new RGBImageUncompressed();
            img.init(32, 32);
            img.createTestPattern();
            return img;
        }

        FileInputStream fis = new FileInputStream(inImageFd);
        BufferedInputStream bis = new BufferedInputStream(fis);

        img = importRGB(bis);

        bis.close();
        return img;
    }

    public RGBAImageUncompressed importRGBA(InputStream is) throws ImageNotRecognizedException, Exception
    {
        RGBAImageUncompressed img;
        img = new RGBAImageUncompressed();

        Bitmap bitmap = null;
        try {
            bitmap = BitmapFactory.decodeStream(is);
            is.close();
          } 
          catch(IOException e) {
        }

        AndroidRGBAImageUncompressedRenderer.importFromAndroidBitmap(bitmap, img);
        bitmap.recycle();

        return img;
    }
}
