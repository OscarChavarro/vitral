//===========================================================================
//= Initial version: august 9 2007

package vsdk.transition.gui;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.media.RGBAImage;
import vsdk.toolkit.media.RGBPixel;
import vsdk.toolkit.media.RGBAPixel;

public class GuiCommand
{
    private String id;
    private String name;
    private String briefDescription;
    private String help; // Could contain HTML tags
    private RGBAImage icon;
    private RGBImage iconTransparency;

    public GuiCommand()
    {
        id = null;
        name = null;
        briefDescription = null;
        help = null;
        icon = null;
        iconTransparency = null;
    }

    public String getId()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }

    public String getBriefDescription()
    {
        return briefDescription;
    }

    public String getHelp()
    {
        return help;
    }

    public RGBAImage getIcon()
    {
        return icon;
    }

    public RGBImage getIconTransparency()
    {
        return iconTransparency;
    }

    public void applyTransparency()
    {
        if ( icon == null || iconTransparency == null ) {
            return;
        }

        int xlimit, ylimit, x, y;

        xlimit = Math.min(icon.getXSize(), iconTransparency.getXSize());
        ylimit = Math.min(icon.getYSize(), iconTransparency.getYSize());

        RGBPixel in;
        RGBAPixel out;
        int r, g, b, a;

        for ( y = 0; y < ylimit; y++ ) {
            for ( x = 0; x < xlimit; x++ ) {
                in = iconTransparency.getPixel(x, y);
                r = VSDK.signedByte2unsignedInteger(in.r);
                g = VSDK.signedByte2unsignedInteger(in.g);
                b = VSDK.signedByte2unsignedInteger(in.b);
                a = (r + g + b) / 3;
                out = icon.getPixel(x, y);
                out.a = VSDK.unsigned8BitInteger2signedByte(a);
                icon.putPixel(x, y, out);
            }
        }
    }

    public void setId(String i)
    {
        id = i;
    }

    public void setName(String n)
    {
        name = n;
    }

    public void setBrief(String b)
    {
        briefDescription = b;
    }

    public void setHelp(String h)
    {
        help = h;
    }

    public void appendToHelp(String h)
    {
        if ( help != null ) {
            help = help + h;
        }
        else {
            help = h;
        }
    }

    public void setIcon(RGBAImage i)
    {
        icon = i;
    }

    public void setIconTransparency(RGBImage i)
    {
        iconTransparency = i;
    }

    public String toString()
    {
        String msg =  "  - Command [" + id + "]:\n";
        msg = msg + "    . Name: " + name + "\n";
        msg = msg + "    . Brief description: " + briefDescription + "\n";
        if ( icon == null ) {
            msg = msg + "    . No icon image\n";
          }
          else {
              msg = msg + "    . Icon image of size (" + icon.getXSize() +
                  ", " + icon.getYSize() + ")\n";
        }
        return msg;
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
