package org.mapfish.print.processor.jasper;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Map;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;

import org.mapfish.print.output.Values;
import org.mapfish.print.processor.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JasperReportBuilder  implements Processor {
    private static final Logger LOGGER = LoggerFactory.getLogger(JasperReportBuilder.class);

    private File directory = new File(".");
    
    @Override
    public Map<String, Object> doProcess(Values values) throws JRException {
        final FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File file, String name) {
                return name.toLowerCase().endsWith(".jrxml");
            }
        };
        for (String jasperFileName : directory.list(filter)) {
            final File jasperFile = new File(directory.getAbsolutePath(), jasperFileName);
            final File buildFile = new File(directory.getAbsolutePath(),
                    jasperFileName.replaceAll("\\.jrxml$", ".jasper"));
            if (!buildFile.exists() || jasperFile.lastModified() > buildFile.lastModified()) {
                LOGGER.info("Building Jasper report: " + jasperFile.getAbsolutePath());
                long start = System.currentTimeMillis();
                JasperCompileManager.compileReportToFile(jasperFile.getAbsolutePath(),
                        buildFile.getAbsolutePath());
                LOGGER.info("Report built in " + (System.currentTimeMillis() - start) + "ms.");
            }
        }
        return null;
    }

    @Override
    public Map<String, String> getOutputMapper() {
        return null;
    }

    public String getDirectory() {
        return directory.getPath();
    }

    public void setDirectory(String directory) {
        this.directory = new File(directory);
    }
}
