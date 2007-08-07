//===========================================================================

import java.awt.Color;
import javax.swing.JPanel;

public class ViewerPanel extends JPanel
{
    private SciVisApplication parent;
    static int lastId = 0;

    public ViewerPanel(SciVisApplication parent)
    {
        this.parent = parent;
        Color c = Color.BLACK;

        switch ( lastId % 13 ) {
          case 1: c = Color.BLACK; break;
          case 2: c = Color.BLUE; break;
          case 3: c = Color.CYAN; break;
          case 4: c = Color.DARK_GRAY; break;
          case 5: c = Color.GRAY; break;
          case 6: c = Color.GREEN; break;
          case 7: c = Color.LIGHT_GRAY; break;
          case 8: c = Color.MAGENTA; break;
          case 9: c = Color.ORANGE; break;
          case 10: c = Color.PINK; break;
          case 11: c = Color.RED; break;
          case 12: c = Color.WHITE; break;
          case 13: c = Color.YELLOW; break;
        }
        setBackground(c);
	lastId++;
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
