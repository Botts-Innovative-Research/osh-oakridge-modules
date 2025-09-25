package com.botts.api.service.bucket;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public interface IResourceHandler {

    String[] getNames();


    void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, SecurityException;


    void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, SecurityException;


    void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException, SecurityException;


    void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException, SecurityException;


    void addSubResource(IResourceHandler resource);


    void addSubResource(IResourceHandler resource, String... names);

}
