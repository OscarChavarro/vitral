package vsdk.toolkit.io;
import java.io.Serial;

import vsdk.toolkit.common.VSDKException;

public class XmlException extends VSDKException {
    @Serial private static final long serialVersionUID = 20060502L;

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
