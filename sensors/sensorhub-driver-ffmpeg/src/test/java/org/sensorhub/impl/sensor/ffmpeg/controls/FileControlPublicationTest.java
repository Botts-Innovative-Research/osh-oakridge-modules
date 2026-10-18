package org.sensorhub.impl.sensor.ffmpeg.controls;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class FileControlPublicationTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void publicationNeverReplacesAnExistingFinalObject() throws Exception {
        Path directory = temporaryFolder.newFolder("no-replace").toPath();
        Path partial = Files.write(directory.resolve(".clip.partial.mp4"),
                "new recording".getBytes(StandardCharsets.UTF_8));
        Path completed = Files.write(directory.resolve("clip.mp4"),
                "existing recording".getBytes(StandardCharsets.UTF_8));

        assertThrows(IOException.class,
                () -> FileControl.publishCompletedPath(partial, completed, ignored -> { }));

        assertArrayEquals("existing recording".getBytes(StandardCharsets.UTF_8),
                Files.readAllBytes(completed));
        assertTrue("Rejected publication must preserve its recoverable partial",
                Files.isRegularFile(partial));
    }

    @Test
    public void failureAfterFinalPublicationNeverDeletesCompletedRecording() throws Exception {
        Path directory = temporaryFolder.newFolder("post-publish-failure").toPath();
        byte[] recording = "complete mp4 bytes".getBytes(StandardCharsets.UTF_8);
        Path partial = Files.write(directory.resolve(".clip.partial.mp4"), recording);
        Path completed = directory.resolve("clip.mp4");

        assertThrows(IOException.class, () -> FileControl.publishCompletedPath(
                partial, completed, ignored -> { throw new IOException("directory fsync failed"); }));

        assertArrayEquals(recording, Files.readAllBytes(completed));
        assertTrue("The hidden alias remains available for caller cleanup", Files.exists(partial));
    }

    @Test
    public void successfulPublicationRemovesOnlyTheHiddenPartialName() throws Exception {
        Path directory = temporaryFolder.newFolder("success").toPath();
        byte[] recording = "complete mp4 bytes".getBytes(StandardCharsets.UTF_8);
        Path partial = Files.write(directory.resolve(".clip.partial.mp4"), recording);
        Path completed = directory.resolve("clip.mp4");

        FileControl.publishCompletedPath(partial, completed, ignored -> { });

        assertArrayEquals(recording, Files.readAllBytes(completed));
        assertFalse(Files.exists(partial));
    }

    @Test
    public void directoryFsyncFallbackIsOnlyWindowsAccessDenied() {
        AccessDeniedException accessDenied = new AccessDeniedException("directory");
        assertTrue(FileControl.isIgnorableWindowsDirectoryFsyncFailure(accessDenied, true));
        assertFalse(FileControl.isIgnorableWindowsDirectoryFsyncFailure(accessDenied, false));
        assertFalse(FileControl.isIgnorableWindowsDirectoryFsyncFailure(
                new IOException("disk failure"), true));
    }
}
