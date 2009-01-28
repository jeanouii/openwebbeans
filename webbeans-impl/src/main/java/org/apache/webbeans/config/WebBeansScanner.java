/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.apache.webbeans.config;

import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import java.util.Set;

import javax.servlet.ServletContext;

import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.logger.WebBeansLogger;
import org.apache.webbeans.util.WebBeansUtil;
import org.scannotation.AnnotationDB;
import org.scannotation.ClasspathUrlFinder;
import org.scannotation.WarUrlFinder;

/**
 * Configures the web application to find beans.
 */
public final class WebBeansScanner
{
    private WebBeansLogger logger = WebBeansLogger.getLogger(WebBeansScanner.class);

    /** Location of the beans.xml files. */
    private Map<String, InputStream> WEBBEANS_XML_LOCATIONS = new HashMap<String, InputStream>();

    //private Map<String, InputStream> EJB_XML_LOCATIONS = new HashMap<String, InputStream>();

    /** Annotation Database */
    private AnnotationDB ANNOTATION_DB = null;

    /** It is configured or not */
    private boolean configure = false;

    private ServletContext servletContext = null;

    public WebBeansScanner()
    {

    }

    /**
     * Configure the Web Beans Container with deployment information and fills
     * annotation database and beans.xml stream database.
     * 
     * @throws WebBeansConfigurationException if any run time exception occurs
     */
    public void scan(ServletContext servletContext) throws WebBeansConfigurationException
    {
        if (!configure)
        {
            this.servletContext = servletContext;

            configureAnnotationDB();
            configure = true;

        }

    }

    /* Configures annotation database */
    private void configureAnnotationDB() throws WebBeansConfigurationException
    {
        try
        {
            Set<URL> arcs = getArchieves();
            URL[] urls = new URL[arcs.size()];
            arcs.toArray(urls);

            if (ANNOTATION_DB == null)
            {
                ANNOTATION_DB = new AnnotationDB();

                ANNOTATION_DB.setScanClassAnnotations(true);
                ANNOTATION_DB.scanArchives(urls);
                ANNOTATION_DB.crossReferenceMetaAnnotations();

            }

        }
        catch (Exception e)
        {
            logger.error("Initializing of the WebBeans container is failed.", e);
            throw new WebBeansConfigurationException(e);
        }

    }

    /* Collects all URLs */
    private Set<URL> getArchieves() throws Exception
    {
        Set<URL> lists = createURLFromMarkerFile();
        URL warUrl = createURLFromWARFile();

        if (warUrl != null)
        {
            lists.add(warUrl);
        }

        return lists;
    }

    /* Creates URLs from the marker file */
    private Set<URL> createURLFromMarkerFile() throws Exception
    {
        Set<URL> listURL = new HashSet<URL>();
        URL[] urls = null;

        // Root with beans.xml marker.
        urls = ClasspathUrlFinder.findResourceBases("META-INF/beans.xml", WebBeansUtil.getCurrentClassLoader());

        if (urls != null)
        {
            for (URL url : urls)
            {

                URL addPath = null;

                String fileDir = url.getFile();
                if (fileDir.endsWith(".jar!/"))
                {
                    fileDir = fileDir.substring(0, fileDir.lastIndexOf("/")) + "/META-INF/beans.xml";
                    addPath = new URL("jar:" + fileDir);
                }
                else
                {
                    addPath = new URL("file:" + url.getFile() + "META-INF/beans.xml");
                }

                listURL.add(url);
                
                WEBBEANS_XML_LOCATIONS.put(addPath.getFile(), addPath.openStream());
            }
        }

        // Scan for ejb-jar.xml
//        URL[] ejbUrls = ClasspathUrlFinder.findResourceBases("META-INF/ejb-jar.xml", WebBeansUtil.getCurrentClassLoader());
//
//        if (ejbUrls != null && ejbUrls.length > 0)
//        {
//            for (URL ejbUrl : ejbUrls)
//            {
//                // ok, beans.xml and ejb-jar.xml is in the same root
//                if (listURL.contains(ejbUrl))
//                {
//                    URL addPath = null;
//
//                    String fileDir = ejbUrl.getFile();
//
//                    if (fileDir.endsWith(".jar!/"))
//                    {
//                        fileDir = fileDir.substring(0, fileDir.lastIndexOf("/")) + "/META-INF/ejb-jar.xml";
//                        addPath = new URL("jar:" + fileDir);
//                    }
//                    else
//                    {
//                        addPath = new URL("file:" + ejbUrl.getFile() + "META-INF/ejb-jar.xml");
//                    }
//
//                    EJB_XML_LOCATIONS.put(addPath.getFile(), addPath.openStream());
//                }
//            }
//        }

        return listURL;
    }

    private URL createURLFromWARFile() throws Exception
    {
        URL url = this.servletContext.getResource("/WEB-INF/beans.xml");

        if (url != null)
        {
            WEBBEANS_XML_LOCATIONS.put(url.getFile(), url.openStream());

            return WarUrlFinder.findWebInfClassesPath(this.servletContext);
        }

        return null;
    }

    /**
     * Gets list of stream that points to the beans.xml file in the specific
     * locations.
     * 
     * @return list of stream
     */
    public Map<String, InputStream> getWEBBEANS_XML_LOCATIONS()
    {
        return WEBBEANS_XML_LOCATIONS;
    }

    /**
     * Gets list of stream that points to the ejb-jar.xml file in the specific
     * locations.
     * 
     * @return list of stream
     */
//    public Map<String, InputStream> getEJB_XML_LOCATIONS()
//    {
//        return EJB_XML_LOCATIONS;
//    }

    /**
     * Gets annotated classes.
     * 
     * @return annotation database
     */
    public AnnotationDB getANNOTATION_DB()
    {
        return ANNOTATION_DB;
    }
}
