package com.botts.api.service.bucket;

import javax.servlet.http.HttpServletRequest;

public interface IObjectHandler extends IResourceHandler {

    String getObjectPattern();

    default boolean canHandleRequest(HttpServletRequest request) {
        return false;
    }

}
