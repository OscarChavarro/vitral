package gui.awt;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;
import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.border.AbstractBorder;

/**
Thin raised / lowered bevel border for buttons, in the Motif style: light
color on the top and left edges, dark color on the bottom and right ones, and
the opposite when the button is pressed or selected.

Motif's own button border reserves 8 pixels around the button (for its bevel
and the ring of the default button), which leaves a wide gap between buttons
placed next to each other. This border only takes the pixels of the bevel, so
adjacent buttons touch each other. Colors are derived from the background of
the button, so it follows the theme in use.
*/
public class AwtCompactBevelButtonBorder extends AbstractBorder
{
    private static final long serialVersionUID = 1L;

    private final int thickness;

    /**
    @param thickness width of the bevel, in pixels
    */
    public AwtCompactBevelButtonBorder(int thickness)
    {
        this.thickness = thickness;
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height)
    {
        boolean lowered = false;

        if ( c instanceof AbstractButton ) {
            ButtonModel model = ((AbstractButton)c).getModel();
            lowered = (model.isArmed() && model.isPressed()) || model.isSelected();
        }

        Color topLeft;
        Color bottomRight;
        if ( lowered ) {
            topLeft = c.getBackground().darker();
            bottomRight = c.getBackground().brighter();
        }
        else {
            topLeft = c.getBackground().brighter();
            bottomRight = c.getBackground().darker();
        }

        Color previous = g.getColor();
        int i;
        for ( i = 0; i < thickness; i++ ) {
            g.setColor(topLeft);
            g.drawLine(x + i, y + i, x + width - 2 - i, y + i);
            g.drawLine(x + i, y + i, x + i, y + height - 2 - i);
            g.setColor(bottomRight);
            g.drawLine(x + i, y + height - 1 - i, x + width - 1 - i, y + height - 1 - i);
            g.drawLine(x + width - 1 - i, y + i, x + width - 1 - i, y + height - 1 - i);
        }
        g.setColor(previous);
    }

    @Override
    public Insets getBorderInsets(Component c)
    {
        return new Insets(thickness, thickness, thickness, thickness);
    }

    @Override
    public Insets getBorderInsets(Component c, Insets insets)
    {
        insets.top = thickness;
        insets.left = thickness;
        insets.bottom = thickness;
        insets.right = thickness;
        return insets;
    }
}
