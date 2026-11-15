package com.botts.impl.service.oscar;

import org.eclipse.jetty.http.MimeTypes;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StaticGuideMimeTypesTest {

    @Test
    public void registersWebVttSrtAndMp4MimeMappingsForStrictNosniffGateway() {
        MimeTypes mimeTypes = new MimeTypes();
        OSCARServiceModule.registerStaticGuideMimeMappings(mimeTypes);

        assertEquals("text/vtt;charset=utf-8", mimeTypes.getMimeByExtension("01-deployment-init-and-credentials.vtt"));
        assertEquals("text/plain;charset=utf-8", mimeTypes.getMimeByExtension("01-deployment-init-and-credentials.srt"));
        assertEquals("video/mp4", mimeTypes.getMimeByExtension("01-deployment-init-and-credentials.mp4"));
    }

    @Test
    public void ignoresNullMimeTypesSafely() {
        OSCARServiceModule.registerStaticGuideMimeMappings(null);
    }
}
