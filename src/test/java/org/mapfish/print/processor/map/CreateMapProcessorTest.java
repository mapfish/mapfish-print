/*
 * Copyright (C) 2014  Camptocamp
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

package org.mapfish.print.processor.map;

import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationFactory;
import org.mapfish.print.config.Template;
import org.mapfish.print.json.PJsonObject;
import org.mapfish.print.output.Values;
import org.springframework.beans.factory.annotation.Autowired;

import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Basic test of the Map processor.
 * <p/>
 * Created by Jesse on 3/26/14.
 */
public class CreateMapProcessorTest extends AbstractMapfishSpringTest {

    @Autowired
    private ConfigurationFactory configurationFactory;

    @Test
    public void testExecute() throws Exception {
        final Configuration config = configurationFactory.getConfig(getFile(CreateMapProcessorTest.class, "basicMapProcessor.yaml"));
        final Template template = config.getTemplate("main");
        PJsonObject requestData = super.parseJSONObjectFromFile(CreateMapProcessorTest.class, "basicMapRequestData.json");
        Values values = new Values(requestData, template);
        template.getProcessorGraph().createTask(values).invoke();

        BufferedImage map = values.getObject("map", BufferedImage.class);
        final File output = new File(config.getDirectory(), "expectedSimpleImage.png");
        ImageIO.write(map, "png", output);
    }
}
