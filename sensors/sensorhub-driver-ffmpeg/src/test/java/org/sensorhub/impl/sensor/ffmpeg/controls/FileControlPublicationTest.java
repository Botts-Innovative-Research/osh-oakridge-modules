package org.sensorhub.impl.sensor.ffmpeg.controls;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class FileControlPublicationTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void recordingReadinessWaitLeavesOuterDeadlineMargin() {
        assertEquals(Duration.ofSeconds(5), FileControl.RECORDING_READINESS_TIMEOUT);
        assertEquals(5, FileControl.MINIMUM_RETAINABLE_VIDEO_PACKETS);
        assertEquals(Duration.ofSeconds(5), FileControl.RECORDING_COMPLETENESS_TIMEOUT);
        assertTrue("Readiness wait must leave time for close and durable publication",
                FileControl.RECORDING_READINESS_TIMEOUT.compareTo(Duration.ofSeconds(8)) < 0);
        assertTrue("Completeness wait must leave time for close and durable publication",
                FileControl.RECORDING_COMPLETENESS_TIMEOUT.compareTo(Duration.ofSeconds(8)) < 0);
    }

    @Test
    public void publicationNeverReplacesAnExistingFinalObject() throws Exception {
        Path directory = temporaryFolder.newFolder("no-replace").toPath();
        Path partial = Files.write(directory.resolve(".clip.partial.mp4"),
                "new recording".getBytes(StandardCharsets.UTF_8));
        Path completed = Files.write(directory.resolve("clip.mp4"),
                "existing recording".getBytes(StandardCharsets.UTF_8));

        IOException collision = assertThrows(IOException.class,
                () -> FileControl.publishCompletedPath(partial, completed, ignored -> { }));

        assertArrayEquals("existing recording".getBytes(StandardCharsets.UTF_8),
                Files.readAllBytes(completed));
        assertTrue("Rejected publication must preserve its recoverable partial",
                Files.isRegularFile(partial));
        assertFalse("A no-replace collision must never claim the old final as this attempt's file",
                collision instanceof FileControl.FinalObjectCreatedException);
    }

    @Test
    public void failureAfterFinalPublicationNeverDeletesCompletedRecording() throws Exception {
        Path directory = temporaryFolder.newFolder("post-publish-failure").toPath();
        byte[] recording = "complete mp4 bytes".getBytes(StandardCharsets.UTF_8);
        Path partial = Files.write(directory.resolve(".clip.partial.mp4"), recording);
        Path completed = directory.resolve("clip.mp4");

        IOException failure = assertThrows(IOException.class, () -> FileControl.publishCompletedPath(
                partial, completed, ignored -> { throw new IOException("directory fsync failed"); }));

        assertArrayEquals(recording, Files.readAllBytes(completed));
        assertTrue("The hidden alias remains available for caller cleanup", Files.exists(partial));
        assertTrue("Post-link failure must expose ownership for coordinator cleanup",
                failure instanceof FileControl.FinalObjectCreatedException);
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

    @Test
    public void inactiveZeroPacketCancellationDeletesHiddenPartialWithoutExistenceLookup()
            throws Exception {
        Path hiddenPartial = Files.write(temporaryFolder.newFolder("inactive-partial").toPath()
                .resolve("clip.partial-token.mp4"), new byte[]{1});
        FileControl.discardInactivePartial("clips/camera/clip.partial.mp4",
                () -> Files.deleteIfExists(hiddenPartial));
        assertFalse(Files.exists(hiddenPartial));
    }

    @Test
    public void inactiveCancellationDelegatesIdempotentDeleteWhenPartialIsAbsent() throws Exception {
        AtomicBoolean deleted = new AtomicBoolean();
        FileControl.discardInactivePartial("clips/camera/missing.partial.mp4",
                () -> deleted.set(true));
        assertTrue("Deletion is addressed directly because partials are hidden from existence APIs",
                deleted.get());
    }

    @Test
    public void inactiveCancellationSurfacesRealDeletionFailure() {
        IOException failure = assertThrows(IOException.class,
                () -> FileControl.discardInactivePartial("clips/camera/clip.partial.mp4",
                        () -> { throw new IOException("disk is read-only"); }));
        assertTrue(failure.getMessage().contains("read-only"));
    }

    @Test
    public void recordingNameMustBeOneSafeRelativeFilename() {
        assertEquals("2026-10-18_04-50-07_890-g2.mp4",
                FileControl.validateRequestedFileName("2026-10-18_04-50-07_890-g2.mp4"));
        assertEquals("clip_2", FileControl.validateRequestedFileName("clip_2"));
        for (String invalid : new String[]{"../escape.mp4", "clips/escape.mp4",
                "clips\\escape.mp4", "/absolute.mp4", "C:\\absolute.mp4",
                "file://escape.mp4", ".", "..", " clip.mp4", "clip.mp4 ",
                "clip.partial-bad.mp4", "clip.txt", "clip.MP4", "bad\0name.mp4"}) {
            assertThrows("Accepted unsafe name: " + invalid, IllegalArgumentException.class,
                    () -> FileControl.validateRequestedFileName(invalid));
        }
    }

    @Test
    public void nonAlarmFinalizationUsesCancellationInsteadOfStrictClose() throws Exception {
        AtomicBoolean closed = new AtomicBoolean();
        AtomicBoolean canceled = new AtomicBoolean();
        FileControl.finishOutput(false, () -> closed.set(true), () -> canceled.set(true));
        assertFalse(closed.get());
        assertTrue(canceled.get());
    }
}
