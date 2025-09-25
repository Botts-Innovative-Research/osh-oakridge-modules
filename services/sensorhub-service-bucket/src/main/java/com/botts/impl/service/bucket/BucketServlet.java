package com.botts.impl.service.bucket;

import com.botts.api.service.bucket.IBucketStore;
import org.sensorhub.api.datastore.DataStoreException;
import org.sensorhub.api.security.IPermission;
import org.sensorhub.impl.module.ModuleSecurity;
import org.slf4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;

public class BucketServlet extends HttpServlet {

    private final String rootUrl;
    private final ExecutorService threadPool;
    private final ModuleSecurity securityHandler;
    private final Logger log;
    private final IBucketStore bucketStore;
    private final BucketSecurity sec;
    private final BucketHandler bucketHandler;
    private final ObjectHandler objectHandler;

    public BucketServlet(BucketService service, ModuleSecurity securityHandler, BucketHandler bucketHandler, ObjectHandler objectHandler) {
        this.sec = (BucketSecurity) securityHandler;
        this.threadPool = service.getThreadPool();
        this.securityHandler = securityHandler;
        this.log = service.getLogger();
        this.bucketStore = service.getBucketStore();

        var endpointUrl = service.getPublicEndpointUrl();
        this.rootUrl = endpointUrl.endsWith("/") ? endpointUrl.substring(0, endpointUrl.length()-1) : endpointUrl;
    }

    private String getBucketName(String pathInfo) {
        return pathInfo.split("/")[1];
    }

    private String getObjectKey(String pathInfo) {
        return pathInfo.split("/", 3)[2];
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
//        if (!sec.hasPermission(sec.api_get)) {
//            showUnauthorized(sec.api_get, req, resp);
//            return;
//        }

        // List all buckets
        if (!sec.hasPermission(sec.api_list)) {
            showUnauthorized(sec.api_get, req, resp);
            return;
        }

        listBuckets(req, resp);
        var pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            long numBuckets = bucketStore.getNumBuckets();

            if (numBuckets == 0) {
                resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
                return;
            }

            try {
                printBuckets(resp.getOutputStream());
            } catch (DataStoreException e) {
                sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, IBucketStore.FAILED_LIST_BUCKETS, req, resp);
            }

            return;
        }

        String[] pathParts = pathInfo.split("/", 3);

        // Check bucket exists
        String bucketName = "";
        if (pathParts.length > 1) {
            bucketName = pathParts[1];
            if (!bucketStore.bucketExists(bucketName)) {
                sendError(HttpServletResponse.SC_NOT_FOUND, IBucketStore.BUCKET_NOT_FOUND, req, resp);
                return;
            }
        }

        // List objects in bucket
        if ((pathParts.length == 2 && !bucketName.isBlank()) || (pathParts.length == 3 && pathParts[2].isBlank())) {
            long numObjects = bucketStore.getNumObjects(bucketName);

            if (numObjects == 0) {
                resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
                return;
            }

            try {
                printObjectKeys(bucketName, resp.getOutputStream());
            } catch (DataStoreException e) {
                sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, IBucketStore.FAILED_LIST_OBJECTS + bucketName, req, resp);
            }

            return;
        }

        String objectKey = pathParts[2];
        if (!bucketStore.objectExists(bucketName, objectKey)) {
            sendError(HttpServletResponse.SC_NOT_FOUND, IBucketStore.OBJECT_NOT_FOUND + bucketName, req, resp);
            return;
        }

        try {
            String mimeType = bucketStore.getObjectMimeType(bucketName, objectKey);
            resp.setContentType(mimeType);
            InputStream objectData = bucketStore.getObject(bucketName, objectKey);
            objectData.transferTo(resp.getOutputStream());
        } catch (DataStoreException e) {
            sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, IBucketStore.FAILED_GET_OBJECT + bucketName, req, resp);
        }
    }

    private void printBuckets(ServletOutputStream out) throws DataStoreException {
        bucketStore.listBuckets().forEach(bucketName -> {
            try {
                out.println(bucketName);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void printObjectKeys(String bucketName, ServletOutputStream out) throws DataStoreException {
        bucketStore.listObjects(bucketName).forEach(objectKey -> {
            try {
                out.println(objectKey);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doPost(req, resp);
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doPut(req, resp);
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doDelete(req, resp);
    }

    public BucketSecurity getSecurityHandler() {
        return sec;
    }
}
