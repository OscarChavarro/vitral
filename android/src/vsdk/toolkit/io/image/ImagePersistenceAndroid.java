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
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.media.RGBAImage;
import vsdk.toolkit.media.RGBPixel;
import vsdk.toolkit.render.android.AndroidRGBImageRenderer;
import vsdk.toolkit.render.android.AndroidRGBAImageRenderer;

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

    public RGBImage importRGB(InputStream is) throws ImageNotRecognizedException, Exception
    {
        RGBImage img;
        img = new RGBImage();

        Bitmap bitmap = null;
        try {
            bitmap = BitmapFactory.decodeStream(is);
            is.close();
          } 
          catch(IOException e) {
        }

        img.init(bitmap.getWidth(), bitmap.getHeight());

        AndroidRGBImageRenderer.importFromAndroidBitmap(bitmap, img);

        bitmap.recycle();

        return img;
    }

    public RGBAImage importRGBA(File inImageFd) throws ImageNotRecognizedException, Exception
    {
        RGBAImage img;

        if ( !inImageFd.exists() ) {
            VSDK.reportMessage(this, VSDK.WARNING,
                "importRGBA", "ERROR: file +\"" + 
			       inImageFd.getPath() + "\" does not exist!");
            img = new RGBAImage();
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

    public RGBImage importRGB(File inImageFd) throws ImageNotRecognizedException, Exception
    {
        RGBImage img;

        if ( !inImageFd.exists() ) {
            VSDK.reportMessage(this, VSDK.WARNING,
                "importRGBA", "ERROR: file +\"" + 
                inImageFd.getPath() + "\" does not exist!");
            img = new RGBImage();
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

    public RGBAImage importRGBA(InputStream is) throws ImageNotRecognizedException, Exception
    {
        RGBAImage img;
        img = new RGBAImage();

        Bitmap bitmap = null;
        try {
            bitmap = BitmapFactory.decodeStream(is);
            is.close();
          } 
          catch(IOException e) {
        }

        AndroidRGBAImageRenderer.importFromAndroidBitmap(bitmap, img);
        bitmap.recycle();

        return img;
    }
}
