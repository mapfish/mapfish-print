package apps;

import javax.imageio.ImageIO;
import javax.media.jai.JAI;
import javax.media.jai.OperationRegistry;
import javax.media.jai.RegistryMode;

public class ListImageIOFormats {
  public static void main(String[] args) {
      for (String format : ImageIO.getWriterFormatNames()) {
          System.out.println(format);
      }

  }
}