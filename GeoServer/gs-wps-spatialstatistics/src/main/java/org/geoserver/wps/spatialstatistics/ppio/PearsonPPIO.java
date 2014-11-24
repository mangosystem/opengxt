/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 MangoSystem
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.spatialstatistics.ppio;

import java.io.InputStream;

import javax.xml.namespace.QName;

import org.geoserver.wps.ppio.XStreamPPIO;
import org.geotools.process.spatialstatistics.operations.PearsonOperation.PearsonResult;
import org.geotools.process.spatialstatistics.operations.PearsonOperation.PearsonResult.PropertyName;
import org.geotools.process.spatialstatistics.operations.PearsonOperation.PearsonResult.PropertyName.PearsonItem;
import org.xml.sax.ContentHandler;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.io.xml.SaxWriter;
import com.thoughtworks.xstream.io.xml.XmlFriendlyNameCoder;
import com.thoughtworks.xstream.mapper.MapperWrapper;

/**
 * A PPIO to generate good looking xml for the StatisticsFeatures process results
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class PearsonPPIO extends XStreamPPIO {

    final XmlFriendlyNameCoder nameCoder = new XmlFriendlyNameCoder("__", "_");

    static final QName PPIO_NAME = new QName("http://www.opengis.net/sld", "Pearson");

    protected PearsonPPIO() {
        super(PearsonResult.class, PPIO_NAME);
    }

    @Override
    protected XStream buildXStream() {
        XStream xstream = new XStream(new DomDriver("UTF-8", nameCoder)) {
            protected boolean useXStream11XmlFriendlyMapper() {
                return true;
            }

            protected MapperWrapper wrapMapper(MapperWrapper next) {
                return new UppercaseTagMapper(next);
            };
        };

        xstream.processAnnotations(PearsonResult.class);
        xstream.processAnnotations(PropertyName.class);
        xstream.processAnnotations(PearsonItem.class);

        xstream.alias("PearsonResult", PearsonResult.class);
        xstream.alias("PropertyName", PropertyName.class);
        xstream.alias("Item", PearsonItem.class);

        return xstream;
    }

    @Override
    public void encode(Object object, ContentHandler handler) throws Exception {
        // bind with the content handler
        SaxWriter writer = new SaxWriter(nameCoder);
        writer.setContentHandler(handler);

        // write out xml
        buildXStream().marshal(object, writer);
    }

    @Override
    public Object decode(InputStream input) throws Exception {
        return buildXStream().fromXML(input);
    }
}
