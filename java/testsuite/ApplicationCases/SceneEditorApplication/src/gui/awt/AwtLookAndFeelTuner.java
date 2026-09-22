package gui.awt;

import javax.swing.UIManager;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.basic.BasicBorders;

/**
Adjusts Swing defaults of the current look and feel to the needs of the
application GUI. It must be called each time a look and feel is set, before
creating the components.

With the Motif look and feel, buttons are compacted: their default border
leaves a wide empty area around each button, so the icons of the toolbox and
the buttons of the side panels appear separated. A thin bevel border is used
instead, so adjacent buttons touch each other. For the rest of look and feels
the adjustment is removed, leaving their own defaults.
*/
public final class AwtLookAndFeelTuner
{
    private static final String MOTIF_ID = "Motif";
    private static final String BUTTON_BORDER_KEY = "Button.border";
    private static final int MOTIF_BEVEL_THICKNESS = 2;

    private AwtLookAndFeelTuner()
    {
    }

    /**
    @return true if the current look and feel is Motif
    */
    public static boolean isMotif()
    {
        return MOTIF_ID.equals(UIManager.getLookAndFeel().getID());
    }

    /**
    Applies (or removes) the adjustments for the current look and feel.
    */
    public static void apply()
    {
        if ( isMotif() ) {
            // The margin border keeps the text margin of the button
            // (`Button.margin`), which the GUI sets to zero for icon buttons
            UIManager.put(BUTTON_BORDER_KEY,
                new BorderUIResource.CompoundBorderUIResource(
                    new AwtCompactBevelButtonBorder(MOTIF_BEVEL_THICKNESS),
                    new BasicBorders.MarginBorder()));
        }
        else {
            // Defaults put by the application override the ones of every
            // look and feel, so they must not stay after leaving Motif
            UIManager.put(BUTTON_BORDER_KEY, null);
        }
    }
}
