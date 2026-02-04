package com.botts.impl.service.oscar.webid;

import com.botts.api.service.bucket.IBucketStore;
import com.botts.impl.service.bucket.handler.DefaultObjectHandler;
import com.botts.impl.service.bucket.util.RequestContext;
import com.botts.impl.service.oscar.cambio.CambioConverter;
import com.google.common.collect.ImmutableCollection;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class WebIdResourceHandler extends DefaultObjectHandler {

    private static final Set<String> WEB_ID_FILE_EXTENSIONS = new CambioConverter().getSupportedInputFormats();

    private final Pattern pattern;

    public WebIdResourceHandler(IBucketStore bucketStore) {
        super(bucketStore);

        String joinedExtensions = String.join("|", WEB_ID_FILE_EXTENSIONS);
        String regex = new StringBuilder(".*\\.(")
                .append(joinedExtensions)
                .append(")")
                .toString();
        pattern = Pattern.compile(regex);
    }

    @Override
    public void doPut(RequestContext ctx) throws IOException, SecurityException {
        super.doPut(ctx);
    }

    @Override
    public void doPost(RequestContext ctx) throws IOException, SecurityException {
        super.doPost(ctx);
        // TODO: Take in query parameters to attach WebID analysis to occupancy, and output WebID analysis to proper lane
        /**
         * occupancyObsId
         * laneUid
         * webIdEnabled
         */

        // TODO: super method, if web enabled then send web ID request, and attach web ID results to occupancy
    }

    @Override
    public String getObjectPattern() {
        return this.pattern.pattern();
    }
}
