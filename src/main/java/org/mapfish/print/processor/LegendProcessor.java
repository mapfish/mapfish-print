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

package org.mapfish.print.processor;

import java.awt.Image;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import net.sf.jasperreports.engine.data.JRTableModelDataSource;

import org.mapfish.print.jasper.TableDataSource;
import org.mapfish.print.json.PJsonArray;
import org.mapfish.print.json.PJsonObject;
import org.mapfish.print.output.Values;

public class LegendProcessor extends AbstractProsessor {
    private String legendRef; 

    @Override
    public Map<String, Object> doProcess(Values values) throws Exception {
        Map<String, Object> output = new HashMap<String, Object>();

        final List<Object[]> legendList = new ArrayList<Object[]>();
        final String[] legendColumns = { "name", "icon", "level" };
        final PJsonObject jsonLegend = (PJsonObject)values.getObject(legendRef);
        fillLegend(jsonLegend, legendList, 0);
        final Object[][] legend = new Object[legendList.size()][];
        output.put("legend", new JRTableModelDataSource(new TableDataSource(legendColumns, legendList.toArray(legend))));

        return output;
    }

    private void fillLegend(PJsonObject jsonLegend, List<Object[]> legendList, int level) throws IOException {
        final String icon = jsonLegend.optString("icon");
        Image image = null;
        if (icon != null) {
            final URL url = new URL(icon);
            image = ImageIO.read(url);
        }

        final Object[] row = { jsonLegend.optString("name"), image, level };
        legendList.add(row);

        PJsonArray jsonClass = jsonLegend.optJSONArray("classes");

        if (jsonClass != null) {
            for (int i = 0; i < jsonClass.size(); i++) {
                fillLegend(jsonClass.getJSONObject(i), legendList, level + 1);
            }
        }
    }
    public String getLegendRef() {
        return legendRef;
    }

    public void setLegendRef(String legendRef) {
        this.legendRef = legendRef;
    }
}
