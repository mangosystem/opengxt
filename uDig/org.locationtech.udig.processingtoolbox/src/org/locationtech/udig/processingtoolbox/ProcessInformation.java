/*
 * uDig - User Friendly Desktop Internet GIS client
 * (C) MangoSystem - www.mangosystem.com 
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html), and the HydroloGIS BSD
 * License v1.0 (http://udig.refractions.net/files/hsd3-v10.html).
 */
package org.locationtech.udig.processingtoolbox;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.geotools.util.logging.Logging;
import org.locationtech.udig.processingtoolbox.internal.Messages;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.io.xml.XmlFriendlyReplacer;

/**
 * Process information & loader class
 * 
 * @author MapPlus
 * 
 */
public class ProcessInformation {
    protected static final Logger LOGGER = Logging.getLogger(ProcessInformation.class);

    final static XmlFriendlyReplacer replacer = new XmlFriendlyReplacer("__", "_"); //$NON-NLS-1$ //$NON-NLS-2$

    // Spatial Statistics - Spatial Distribution - Mean Center

    @XStreamImplicit(itemFieldName = "Category")
    private List<Category> items = new ArrayList<Category>();

    public List<Category> getItems() {
        return items;
    }

    public void setItems(List<Category> items) {
        this.items = items;
    }

    public static final class Category {
        public String Name = Messages.ProcessInformation_Others;

        @XStreamImplicit(itemFieldName = "SubCategory")
        private List<SubCategory> items = new ArrayList<SubCategory>();

        public List<SubCategory> getItems() {
            return items;
        }

        public void setItems(List<SubCategory> items) {
            this.items = items;
        }

        public Category() {
        }

        public Category(String name) {
            this.Name = name;
        }
    }

    public static final class SubCategory {
        public String Name = Messages.ProcessInformation_Others;

        @XStreamImplicit(itemFieldName = "Process")
        private List<String> items = new ArrayList<String>();

        public List<String> getItems() {
            return items;
        }

        public void setItems(List<String> items) {
            this.items = items;
        }

        public SubCategory() {
        }

        public SubCategory(String name) {
            this.Name = name;
        }
    }

    public static ProcessInformation decode(File file) {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            return decode(fis);
        } catch (FileNotFoundException e) {
            LOGGER.log(Level.WARNING, e.getMessage(), e);
        } finally {
            IOUtils.closeQuietly(fis);
        }

        return new ProcessInformation();
    }

    public static ProcessInformation decode(InputStream is) {
        XStream xstream = new XStream(new DomDriver("UTF-8", replacer)) { //$NON-NLS-1$
            @Override
            protected boolean useXStream11XmlFriendlyMapper() {
                return true;
            }
        };

        xstream.processAnnotations(ProcessInformation.class);
        xstream.processAnnotations(SubCategory.class);
        xstream.alias("ProcessInformation", ProcessInformation.class); //$NON-NLS-1$
        xstream.alias("Category", Category.class); //$NON-NLS-1$
        xstream.alias("SubCategory", SubCategory.class); //$NON-NLS-1$

        return (ProcessInformation) xstream.fromXML(is);
    }

    public static void encode(ProcessInformation pi) {
        XStream xstream = new XStream(new DomDriver("UTF-8", replacer)) { //$NON-NLS-1$
            @Override
            protected boolean useXStream11XmlFriendlyMapper() {
                return true;
            }
        };

        xstream.processAnnotations(ProcessInformation.class);
        xstream.processAnnotations(SubCategory.class);
        xstream.alias("ProcessInformation", ProcessInformation.class); //$NON-NLS-1$
        xstream.alias("Category", Category.class); //$NON-NLS-1$
        xstream.alias("SubCategory", SubCategory.class); //$NON-NLS-1$
    }
}
