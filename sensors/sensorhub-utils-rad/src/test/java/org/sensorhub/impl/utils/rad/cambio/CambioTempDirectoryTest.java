package org.sensorhub.impl.utils.rad.cambio;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;

public class CambioTempDirectoryTest {

    @Test
    public void usesManagedDataTempDirectoryForInputAndOutput() throws Exception {
        Path dataTemp = Files.createTempDirectory("cambio-data-tmp-").toRealPath();
        Path nativeTemp = Files.createTempDirectory("cambio-native-tmp-").toRealPath();
        String oldDataTemp = System.getProperty("oscar.data.tmpdir");
        String oldJavaTemp = System.getProperty("java.io.tmpdir");
        File input = null;
        File output = null;
        try {
            System.setProperty("oscar.data.tmpdir", dataTemp.toString());
            System.setProperty("java.io.tmpdir", nativeTemp.toString());

            CambioConverter converter = new CambioConverter();
            input = converter.createTempFile(
                new ByteArrayInputStream("spectrum".getBytes(StandardCharsets.UTF_8)), "sample.n42");
            output = CambioConverter.createDataTempFile("cambio_output_", ".n42");

            assertEquals(dataTemp.toFile(), input.getParentFile().getCanonicalFile());
            assertEquals(dataTemp.toFile(), output.getParentFile().getCanonicalFile());
            assertNotEquals(nativeTemp.toFile(), input.getParentFile().getCanonicalFile());
            assertNotEquals(nativeTemp.toFile(), output.getParentFile().getCanonicalFile());
        } finally {
            if (input != null)
                Files.deleteIfExists(input.toPath());
            if (output != null)
                Files.deleteIfExists(output.toPath());
            restoreSystemProperty("oscar.data.tmpdir", oldDataTemp);
            restoreSystemProperty("java.io.tmpdir", oldJavaTemp);
            deleteDirectory(dataTemp);
            deleteDirectory(nativeTemp);
        }
    }

    @Test
    public void rejectsRelativeManagedDataTempDirectory() {
        String oldDataTemp = System.getProperty("oscar.data.tmpdir");
        try {
            System.setProperty("oscar.data.tmpdir", "relative-cambio-temp");
            assertThrows(IOException.class,
                () -> CambioConverter.createDataTempFile("cambio_output_", ".n42"));
        } finally {
            restoreSystemProperty("oscar.data.tmpdir", oldDataTemp);
        }
    }

    @Test
    public void rejectsBlankOrNativeManagedDataTempDirectory() throws Exception {
        String oldDataTemp = System.getProperty("oscar.data.tmpdir");
        String oldJavaTemp = System.getProperty("java.io.tmpdir");
        Path nativeTemp = Files.createTempDirectory("cambio-native-only-").toRealPath();
        try {
            System.setProperty("java.io.tmpdir", nativeTemp.toString());

            System.setProperty("oscar.data.tmpdir", "\t");
            IOException blank = assertThrows(IOException.class,
                () -> CambioConverter.createDataTempFile("cambio_output_", ".n42"));
            assertEquals("oscar.data.tmpdir must not be blank", blank.getMessage());

            System.setProperty("oscar.data.tmpdir", nativeTemp.toString());
            IOException shared = assertThrows(IOException.class,
                () -> CambioConverter.createDataTempFile("cambio_output_", ".n42"));
            assertEquals("oscar.data.tmpdir must differ from java.io.tmpdir", shared.getMessage());
        } finally {
            restoreSystemProperty("oscar.data.tmpdir", oldDataTemp);
            restoreSystemProperty("java.io.tmpdir", oldJavaTemp);
            deleteDirectory(nativeTemp);
        }
    }

    private static void restoreSystemProperty(String name, String value) {
        if (value == null)
            System.clearProperty(name);
        else
            System.setProperty(name, value);
    }

    private static void deleteDirectory(Path directory) throws IOException {
        if (!Files.exists(directory))
            return;
        try (var paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        }
    }
}
