/*
 * Copyright (C) 2013  Camptocamp
 *
 * This file is part of MapFish Print
 *
 * MapFish Print is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MapFish Print is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MapFish Print.  If not, see <http://www.gnu.org/licenses/>.
 */

package apps;

import java.awt.RenderingHints;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;

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
