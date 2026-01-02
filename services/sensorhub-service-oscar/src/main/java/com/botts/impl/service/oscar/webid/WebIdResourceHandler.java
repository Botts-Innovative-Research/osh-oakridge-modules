package com.botts.impl.service.oscar.webid;

import com.botts.api.service.bucket.IBucketStore;
import com.botts.impl.service.bucket.handler.DefaultObjectHandler;
import com.google.common.collect.ImmutableCollection;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class WebIdResourceHandler extends DefaultObjectHandler {

    private static final Set<String> WEB_ID_FILE_EXTENSIONS = Set.of(
            "n42",
            ""
    );

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
    public String getObjectPattern() {
        return this.pattern.pattern();
    }
}
