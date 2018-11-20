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
        final File f1 = new File(reportDir, "1");
        f1.createNewFile();
        f1.setLastModified(oldDate);
        // old file, should be deleted
        final File f2 = new File(reportDir, "2");
        f2.createNewFile();
        f2.setLastModified(oldDate);
        // new file, should be kept
        final File f3 = new File(reportDir, "3");
        f3.createNewFile();

        // old task dir, should be deleted
        File taskDir1 = new File(workingDir, "task-1tmp");
        taskDir1.mkdirs();
        new File(taskDir1, "123.tmp").createNewFile();
        new File(workingDir, "task-1tmp").setLastModified(oldDate);
        // new task dir, should be kept
        File taskDir2 = new File(workingDir, "task-2tmp");
        taskDir2.mkdirs();
        new File(taskDir2, "123.tmp").createNewFile();

        assertTrue(f1.exists());
        assertTrue(f2.exists());
        assertTrue(f3.exists());
        assertTrue(taskDir1.exists());
        assertTrue(taskDir2.exists());

        int maxAgeInSeconds = 1000;
        this.workingDirectories.new CleanUpTask(maxAgeInSeconds, maxAgeInSeconds).run();

        assertFalse(f1.exists());
        assertFalse(f2.exists());
        assertTrue(f3.exists());
        assertFalse(taskDir1.exists());
        assertTrue(taskDir2.exists());
    }
}
