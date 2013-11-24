//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - December 31 2007 - Oscar Chavarro: Original base version              =
//===========================================================================

package vsdk.toolkit.gui;

// J2ME classes
import javax.microedition.lcdui.Canvas;

/**
This class gives VitralSDK access to GUI operations in the J2meSystem, as
translation of J2me specific events to VitralSDK generalized / portable
events.
*/
public class J2meSystem extends PresentationElement
{
    public static KeyEvent j2me2vsdkEvent(Canvas context, int keycode)
    {
        KeyEvent evsdk;

        evsdk = new KeyEvent();
        j2me2vsdkKeyEvent(context, evsdk, keycode);
        return evsdk;
    }

    public static void j2me2vsdkKeyEvent(Canvas context,  KeyEvent evsdk, int keycode)
    {
        if ( keycode < 0 ) {
            int action = context.getGameAction(keycode);
            switch ( action ) {
              case Canvas.UP:
                evsdk.keycode = KeyEvent.KEY_UP;
                break;
              case Canvas.DOWN:
                evsdk.keycode = KeyEvent.KEY_DOWN;
                break;
              case Canvas.LEFT:
                evsdk.keycode = KeyEvent.KEY_LEFT;
                break;
              case Canvas.RIGHT:
                evsdk.keycode = KeyEvent.KEY_RIGHT;
                break;
            }
        }
        else {
            char unicode_id;
            unicode_id = (char)keycode;

            switch ( unicode_id ) {
              case 'A':
                evsdk.keycode = KeyEvent.KEY_A;
                break;
              case 'B':
                evsdk.keycode = KeyEvent.KEY_B;
                break;
              case 'C':
                evsdk.keycode = KeyEvent.KEY_C;
                break;
              case 'D':
                evsdk.keycode = KeyEvent.KEY_D;
                break;
              case 'E':
                evsdk.keycode = KeyEvent.KEY_E;
                break;
              case 'F':
                evsdk.keycode = KeyEvent.KEY_F;
                break;
              case 'G':
                evsdk.keycode = KeyEvent.KEY_G;
                break;
              case 'H':
                evsdk.keycode = KeyEvent.KEY_H;
                break;
              case 'I':
                evsdk.keycode = KeyEvent.KEY_I;
                break;
              case 'J':
                evsdk.keycode = KeyEvent.KEY_J;
                break;
              case 'K':
                evsdk.keycode = KeyEvent.KEY_K;
                break;
              case 'L':
                evsdk.keycode = KeyEvent.KEY_L;
                break;
              case 'M':
                evsdk.keycode = KeyEvent.KEY_M;
                break;
              case 'N':
                evsdk.keycode = KeyEvent.KEY_N;
                break;
              case 'O':
                evsdk.keycode = KeyEvent.KEY_O;
                break;
              case 'P':
                evsdk.keycode = KeyEvent.KEY_P;
                break;
              case 'Q':
                evsdk.keycode = KeyEvent.KEY_Q;
                break;
              case 'R':
                evsdk.keycode = KeyEvent.KEY_R;
                break;
              case 'S':
                evsdk.keycode = KeyEvent.KEY_S;
                break;
              case 'T':
                evsdk.keycode = KeyEvent.KEY_T;
                break;
              case 'U':
                evsdk.keycode = KeyEvent.KEY_U;
                break;
              case 'V':
                evsdk.keycode = KeyEvent.KEY_V;
                break;
              case 'W':
                evsdk.keycode = KeyEvent.KEY_W;
                break;
              case 'X':
                evsdk.keycode = KeyEvent.KEY_X;
                break;
              case 'Y':
                evsdk.keycode = KeyEvent.KEY_Y;
                break;
              case 'Z':
                evsdk.keycode = KeyEvent.KEY_Z;
                break;
              case 'a':
                evsdk.keycode = KeyEvent.KEY_a;
                break;
              case 'b':
                evsdk.keycode = KeyEvent.KEY_b;
                break;
              case 'c':
                evsdk.keycode = KeyEvent.KEY_c;
                break;
              case 'd':
                evsdk.keycode = KeyEvent.KEY_d;
                break;
              case 'e':
                evsdk.keycode = KeyEvent.KEY_e;
                break;
              case 'f':
                evsdk.keycode = KeyEvent.KEY_f;
                break;
              case 'g':
                evsdk.keycode = KeyEvent.KEY_g;
                break;
              case 'h':
                evsdk.keycode = KeyEvent.KEY_h;
                break;
              case 'i':
                evsdk.keycode = KeyEvent.KEY_i;
                break;
              case 'j':
                evsdk.keycode = KeyEvent.KEY_j;
                break;
              case 'k':
                evsdk.keycode = KeyEvent.KEY_k;
                break;
              case 'l':
                evsdk.keycode = KeyEvent.KEY_l;
                break;
              case 'm':
                evsdk.keycode = KeyEvent.KEY_m;
                break;
              case 'n':
                evsdk.keycode = KeyEvent.KEY_n;
                break;
              case 'o':
                evsdk.keycode = KeyEvent.KEY_o;
                break;
              case 'p':
                evsdk.keycode = KeyEvent.KEY_p;
                break;
              case 'q':
                evsdk.keycode = KeyEvent.KEY_q;
                break;
              case 'r':
                evsdk.keycode = KeyEvent.KEY_r;
                break;
              case 's':
                evsdk.keycode = KeyEvent.KEY_s;
                break;
              case 't':
                evsdk.keycode = KeyEvent.KEY_t;
                break;
              case 'u':
                evsdk.keycode = KeyEvent.KEY_u;
                break;
              case 'v':
                evsdk.keycode = KeyEvent.KEY_v;
                break;
              case 'w':
                evsdk.keycode = KeyEvent.KEY_w;
                break;
              case 'x':
                evsdk.keycode = KeyEvent.KEY_x;
                break;
              case 'y':
                evsdk.keycode = KeyEvent.KEY_y;
                break;
              case 'z':
                evsdk.keycode = KeyEvent.KEY_z;
                break;
              case '0':
                evsdk.keycode = KeyEvent.KEY_0;
                break;
              case '1':
                evsdk.keycode = KeyEvent.KEY_1;
                break;
              case '2':
                evsdk.keycode = KeyEvent.KEY_2;
                break;
              case '3':
                evsdk.keycode = KeyEvent.KEY_3;
                break;
              case '4':
                evsdk.keycode = KeyEvent.KEY_4;
                break;
              case '5':
                evsdk.keycode = KeyEvent.KEY_5;
                break;
              case '6':
                evsdk.keycode = KeyEvent.KEY_6;
                break;
              case '7':
                evsdk.keycode = KeyEvent.KEY_7;
                break;
              case '8':
                evsdk.keycode = KeyEvent.KEY_8;
                break;
              case '9':
                evsdk.keycode = KeyEvent.KEY_9;
                break;
            }
        }
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
