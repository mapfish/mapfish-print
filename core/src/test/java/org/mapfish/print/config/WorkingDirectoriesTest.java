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
        File workingDir = this.workingDirectories.getWorking();
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

        // old task dir, should be deleted
        File taskDir1 = new File(workingDir, "task-1tmp");
        taskDir1.mkdirs();
        new File(taskDir1, "123.tmp").createNewFile();
        new File(workingDir, "task-1tmp").setLastModified(oldDate);
        // new task dir, should be kept
        File taskDir2 = new File(workingDir, "task-2tmp");
        taskDir2.mkdirs();
        new File(taskDir2, "123.tmp").createNewFile();

        assertTrue(new File(reportDir, "1").exists());
        assertTrue(new File(reportDir, "2").exists());
        assertTrue(new File(reportDir, "3").exists());
        assertTrue(taskDir1.exists());
        assertTrue(taskDir2.exists());

        int maxAgeInSeconds = 1000 ;
        this.workingDirectories.new CleanUpTask(maxAgeInSeconds, maxAgeInSeconds).run();

        assertFalse(new File(reportDir, "1").exists());
        assertFalse(new File(reportDir, "2").exists());
        assertTrue(new File(reportDir, "3").exists());
        assertFalse(taskDir1.exists());
        assertTrue(taskDir2.exists());
    }
}
