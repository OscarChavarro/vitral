//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - May 17 2006 - Gina Chiquillo: Original base version                   =
//===========================================================================

package vsdk.toolkit.io;

import javax.xml.transform.TransformerFactoryConfigurationError;

public class XmlException extends Exception {

   public XmlException() {
   }

   public XmlException(String message) {
       super(message);
   }

   public XmlException(String message, Throwable cause) {
       super(message, cause);
   }

   public XmlException(Throwable cause) {
       super(cause);
   }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
