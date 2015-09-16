/*
 * Copyright (c) 2004-2006 Auster Solutions do Brasil. All Rights Reserved.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE 
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
 * 
 * Created on 21/02/2006
 */

package br.com.auster.om.filter;

import java.util.Map;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import br.com.auster.common.data.AggregationEngine;
import br.com.auster.common.util.I18n;
import br.com.auster.common.xml.DOMUtils;
import br.com.auster.dware.graph.ConnectException;
import br.com.auster.dware.graph.DefaultFilter;
import br.com.auster.dware.graph.FilterException;
import br.com.auster.dware.graph.ObjectProcessor;
import br.com.auster.dware.graph.Request;


/**
 * <p>
 * <b>Title:</b> AggregationEngineFilter
 * </p>
 * <p>
 * <b>Description:</b> A filter to allow data aggregation using an Aggregation
 * Engine
 * </p>
 * <p>
 * <b>Copyright:</b> Copyright (c) 2005
 * </p>
 * <p>
 * <b>Company:</b> Auster Solutions
 * </p>
 * 
 * @author etirelli
 * @version $Id: AggregationEngineFilter.java 403 2007-03-01 17:43:48Z framos $
 */
public class AggregationEngineFilter extends DefaultFilter implements ObjectProcessor {

	
    private static final String FILTER_CONFIG_ENGINE_PLUGIN       = "aggregation-engine";
    private static final String FILTER_CONFIG_ENGINE_PLUGIN_CLASS = "class-name";
    private static final String FILTER_CONFIG_ENGINE_CONFIG_FILE  = "config-file";
    private static final String FILTER_CONFIG_ENGINE_ENCRYPTED    = "encrypted";

    private static final String CONFIG_REQUEST_TAG                = "request-tag";
    private static final String CONFIG_NAME_ATTR                  = "name";

	private static final I18n i18n = I18n.getInstance(AggregationEngineFilter.class);
	private static final Logger log = Logger.getLogger(AggregationEngineFilter.class);

    private AggregationEngine   engine = null;
    private Request request = null;
    private String  requestTag = null;
    private ObjectProcessor objProcessor = null;

    
    
    /**
     * A constructor that receives a name parameter
     * 
     * @param _name
     */
    public AggregationEngineFilter(String _name) {
        super(_name);
    }

    /**
     * @inheritDoc
     */
    public void prepare(Request request) throws FilterException {
        this.request = request;
    }

    /**
     * <P>
     * Walks through the <code>_configuration</code> parameter looking for the
     * invoice rules engine plugin class.
     * </P>
     * 
     * @param _configuration
     *            the root configuration DOM element
     * 
     * @exception FilterException
     *                if anything when wrong while reading the configuration
     */
    public void configure(Element _configuration) {
        log.debug("Configuring AggregationEngineFilter");
        Element engineElement = DOMUtils.getElement(_configuration, FILTER_CONFIG_ENGINE_PLUGIN, true);
        String engineKlass = DOMUtils.getAttribute(engineElement, FILTER_CONFIG_ENGINE_PLUGIN_CLASS, true);
        String aeConfig = DOMUtils.getAttribute(engineElement, FILTER_CONFIG_ENGINE_CONFIG_FILE, true);
        boolean aeEncrypt = DOMUtils.getBooleanAttribute(engineElement, FILTER_CONFIG_ENGINE_ENCRYPTED, true);
        log.debug("AggregationEngine class=[" + engineKlass + "], config=[" + aeConfig + "], encrypted=[" + aeEncrypt + "]");
        try {
            Object o = Class.forName(engineKlass).newInstance();
            this.engine = (AggregationEngine) o;
            Element aeConf = DOMUtils.openDocument(aeConfig, aeEncrypt);
            this.engine.configure(aeConf);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Element requestElem = DOMUtils.getElement(_configuration, CONFIG_REQUEST_TAG, true);
        requestTag = DOMUtils.getAttribute(requestElem, CONFIG_NAME_ATTR, true);
    }

    /**
     * @inheritDoc
     */
    public Object getInput(String filterName) throws ConnectException, UnsupportedOperationException {
        return this;
    }

    /**
     * @inheritDoc
     * 
     * @param _filterName
     * @param _output
     * @return
     * @throws ConnectException
     * @throws UnsupportedOperationException
     */
    public Object getOutput(String _filterName, Object _output)  throws ConnectException, UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the Output for this filter.
     */
    public void setOutput(String sourceName, Object objProcessor) {
        this.objProcessor = (ObjectProcessor) objProcessor;
    }

    /**
     * @inheritDoc
     */
    public void processElement(Object object) throws FilterException {
    	log.info(i18n.getString("allFilters.startProcessing", this.getClass().getSimpleName(), this.filterName));
        long start = System.currentTimeMillis();
        if (this.objProcessor != null) {
            try {
                Map<String, Object> dataMap = (Map<String, Object>) object;
                dataMap.put(requestTag, request);
                Map results = engine.aggregate(dataMap);
                dataMap.put(requestTag, null);
                dataMap.putAll(results);
                long time = (System.currentTimeMillis()-start);
                log.info(i18n.getString("allFilters.endProcessing", this.getClass().getSimpleName(), this.filterName, String.valueOf(time)));
                this.objProcessor.processElement(dataMap);
            } catch (Exception e) {
                throw new FilterException("Error retrieving data", e);
            }
        }        
    }

}
