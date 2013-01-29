/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.webconsole.internal.core;

import java.io.IOException;
import java.util.regex.Pattern;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.util.tracker.ServiceTracker;

import com.sun.org.apache.xalan.internal.xsltc.compiler.Constants;

/** Provides overridable resources to the Web console: Servlets can
 *  be registered to replace these resources by their own.
 */
public class OverridableResourcesServlet extends HttpServlet
{
    /** Default alias for this Servlet */
    public static final String DEFAULT_ALIAS = "/overridable";
    
    /** If a Servlet service with this property set to true is available,
     *  use it instead of this one.
     */
    public static final String SERVLET_SERVICE_PROPERTY = "org.apache.felix.resources.servlet";
    
    /** Return an empty response by default for paths that match this expression */
    public static final Pattern DEFAULT_EMPTY_RESPONSE_PATTERN = Pattern.compile("/scripts/.*");
    
    private ServiceTracker servletTracker;
    
    public void activate(BundleContext ctx) throws InvalidSyntaxException {
        // Track Servlets that have SERVLET_SERVICE_PROPERTY == true
        final Filter f = ctx.createFilter("(&" + 
                "(objectclass=" + Servlet.class.getName() + ")" + 
                "(" + SERVLET_SERVICE_PROPERTY + "=true)" + 
                ")");
        servletTracker = new ServiceTracker(ctx, f, null);
        servletTracker.open();
    }
    
    public void deactivate() {
        servletTracker.close();
    }
    
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        final String path = req.getPathInfo();
        
        final Servlet delegate = (Servlet)servletTracker.getService();
        
        if(delegate != null) {
            // We have an overriding servlet - let it process the request
            delegate.service(req, res);
            
        } else if(DEFAULT_EMPTY_RESPONSE_PATTERN.matcher(path).matches()) {
            // No specific servlet and path matches default pattern -> empty response.
            // Meant to return empty scripts when they are not overridden.
            res.setContentType(getServletContext().getMimeType(path));
            res.getWriter().flush();
            
        } else {
            // Not found
            final String msg = "No overriding Servlet, and path does not match " + DEFAULT_EMPTY_RESPONSE_PATTERN;
            res.sendError(HttpServletResponse.SC_NOT_FOUND, msg);
        }
    }
}
