package apps;

import javax.imageio.ImageIO;
import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.MosaicDescriptor;
import java.awt.*;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

/**
 * User: jeichar
 * Date: Oct 19, 2010
 * Time: 11:25:16 AM
 */
public class MosiacImages {
    private static final int MARGIN = 50;

    public static void main(String[] args) throws IOException {
        File[] imageFiles = new File("/tmp").listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.startsWith("pdf");
            }
        });

        ImageLayout layout = new ImageLayout();
        layout.setTileWidth(500);
        layout.setTileHeight(500);

        ParameterBlock pbMosaic=new ParameterBlock();

        float height = 0;

        for (File imageFile : imageFiles) {
            PlanarImage source = JAI.create("fileload", imageFile.getPath());
            ParameterBlock pbTranslate=new ParameterBlock();
            pbTranslate.addSource(source);
            pbTranslate.add(0f);
            pbTranslate.add(height);
            RenderedOp translated = JAI.create("translate", pbTranslate, new RenderingHints(JAI.KEY_IMAGE_LAYOUT,layout));

            pbMosaic.addSource(translated);

            height += source.getHeight() + MARGIN;
        }

        RenderedOp mosaic = JAI.create("mosaic", pbMosaic, new RenderingHints(JAI.KEY_IMAGE_LAYOUT,layout));

        ImageIO.write(mosaic, "png",new File("/tmp/mosaic-img.png"));
    }
}
