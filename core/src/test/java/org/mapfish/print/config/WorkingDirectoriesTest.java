package org.mapfish.print.config;

import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WorkingDirectoriesTest extends AbstractMapfishSpringTest {

    @Autowired
    private WorkingDirectories workingDirectories;

    @Test
    public void testCleanUp() throws IOException {
        File reportDir = this.workingDirectories.getReports();

        long oldDate = new Date().getTime() - TimeUnit.DAYS.toMillis(1);

        // old file, should be deleted
        new File(reportDir, "1").createNewFile();
        new File(reportDir, "1").setLastModified(oldDate);
        // old file, should be deleted
        new File(reportDir, "2").createNewFile();
        new File(reportDir, "2").setLastModified(oldDate);
        // new file, should be kept
        new File(reportDir, "3").createNewFile();

        assertTrue(new File(reportDir, "1").exists());
        assertTrue(new File(reportDir, "2").exists());
        assertTrue(new File(reportDir, "3").exists());

        int maxAgeInSeconds = 1000 ;
        this.workingDirectories.new CleanUpTask(maxAgeInSeconds).run();

        assertFalse(new File(reportDir, "1").exists());
        assertFalse(new File(reportDir, "2").exists());
        assertTrue(new File(reportDir, "3").exists());
    }
}
