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

package org.mapfish.print.processor.jasper;

import net.sf.jasperreports.engine.data.JRTableModelDataSource;
import org.mapfish.print.attribute.LegendAttribute.LegendAttributeValue;
import org.mapfish.print.json.PJsonArray;
import org.mapfish.print.json.PJsonObject;
import org.mapfish.print.processor.AbstractProcessor;

import java.awt.Image;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Create a legend.
 *
 * @author Jesse
 * @author sbrunner
 */
public class LegendProcessor extends AbstractProcessor<LegendProcessor.Input, LegendProcessor.Output> {

    private static final String NAME_COLUMN = "name";
    private static final String ICON_COLUMN = "icon";
    private static final String LEVEL_COLUMN = "level";

    private static final String JSON_NAME = "name";
    private static final String JSON_ICONS = "icons";
    private static final String JSON_CLASSES = "classes";

    /**
     * Constructor.
     */
    protected LegendProcessor() {
        super(Output.class);
    }

    @Override
    public final Input createInputParameter() {
        return new Input();
    }

    @Override
    public final Output execute(final Input values) throws Exception {

        final List<Object[]> legendList = new ArrayList<Object[]>();
        final String[] legendColumns = {NAME_COLUMN, ICON_COLUMN, LEVEL_COLUMN};
        final PJsonObject jsonLegend = values.legend.getJsonObject();
        fillLegend(jsonLegend, legendList, 0);
        final Object[][] legend = new Object[legendList.size()][];

        final JRTableModelDataSource dataSource = new JRTableModelDataSource(new TableDataSource(legendColumns,
                legendList.toArray(legend)));
        return new Output(dataSource);
    }

    private void fillLegend(final PJsonObject jsonLegend, final List<Object[]> legendList, final int level) throws IOException {
        final Object[] row = {jsonLegend.optString(JSON_NAME), null, level};
        legendList.add(row);

        final PJsonArray icons = jsonLegend.optJSONArray(JSON_ICONS);
        if (icons != null) {
            for (int i = 0; i < icons.size(); i++) {
                final URL url = new URL(icons.getString(i));
                final Image image = ImageIO.read(url);
                final Object[] iconRow = {null, image, level};
                legendList.add(iconRow);
            }
        }

        PJsonArray jsonClass = jsonLegend.optJSONArray(JSON_CLASSES);

        if (jsonClass != null) {
            for (int i = 0; i < jsonClass.size(); i++) {
                fillLegend(jsonClass.getJSONObject(i), legendList, level + 1);
            }
        }
    }

    /**
     * The Input Parameter object for {@link org.mapfish.print.processor.jasper.LegendProcessor}.
     */
    public static final class Input {
        /**
         * The data required for creating the legend.
         */
        public LegendAttributeValue legend;
    }

    /**
     * The Output object of the legend processor method.
     */
    public static final class Output {
        /**
         * The datasource for the legend object in the report.
         */
        public final JRTableModelDataSource legend;

        Output(final JRTableModelDataSource legend) {
            this.legend = legend;
        }
    }
}
