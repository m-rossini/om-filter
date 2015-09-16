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
 * Created on 27/01/2006
 */
package br.com.auster.om.filter;

import java.net.ConnectException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import br.com.auster.common.util.I18n;
import br.com.auster.common.xml.DOMUtils;
import br.com.auster.dware.graph.DefaultFilter;
import br.com.auster.dware.graph.FilterException;
import br.com.auster.dware.graph.ObjectProcessor;
import br.com.auster.om.invoice.InvoiceObjectModelLoader;

/**
 * <p><b>Title:</b> InvoiceLoaderFilter</p>
 * <p><b>Description:</b> A filter to instantiate an Invoice OM loader and make
 * use of it</p>
 * <p><b>Copyright:</b> Copyright (c) 2005</p>
 * <p><b>Company:</b> Auster Solutions</p>
 *
 * @author etirelli
 * @version $Id: InvoiceLoaderFilter.java 420 2007-04-18 14:05:18Z framos $
 */
public class InvoiceLoaderFilter extends DefaultFilter implements ContentHandler {
	
	private static final I18n i18n = I18n.getInstance(InvoiceLoaderFilter.class);
	private static final Logger log = Logger.getLogger(InvoiceLoaderFilter.class);
  
	public static final String FILTER_CONFIG_LOADER_PLUGIN         = "invoice-loader";
	public static final String FILTER_CONFIG_LOADER_PLUGIN_CLASS   = "class-name";
	public static final String FILTER_CONFIG_OBJECT_LIST           = "object-list-tag";
	public static final String FILTER_CONFIG_OBJECT_LIST_TAG       = "name";
  
	private String listTag = null;
  
	private ContentHandler handler = null;
	private InvoiceObjectModelLoader loader = null;
	private ObjectProcessor objProcessor;
  	// used to calculate the time this filter spent processing
	private long start;
  
  
  
  public InvoiceLoaderFilter(String _name) {
    super(_name);
  } 
  
  /**
   * <P>
   *  Walks through the <code>_configuration</code> parameter looking for the invoice
   * loader plugin class.</P>
   * 
   * @param _configuration the root configuration DOM element
   * 
   * @exception FilterException if anything when wrong while reading the configuration
   */
  public void configure(Element _configuration) {
    log.debug("Configuring InvoiceLoaderFilter");
    Element loaderElement = DOMUtils.getElement(_configuration,FILTER_CONFIG_LOADER_PLUGIN, true);
    String loaderKlass = DOMUtils.getAttribute(loaderElement, FILTER_CONFIG_LOADER_PLUGIN_CLASS, true);
    
    log.debug("Chosen invoice loader class=["+loaderKlass+"]");
    try {
      this.loader = (InvoiceObjectModelLoader) Class.forName(loaderKlass).newInstance();
      this.loader.configure(loaderElement);
      this.handler = (ContentHandler) this.loader;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    
    Element listElement = DOMUtils.getElement(_configuration,FILTER_CONFIG_OBJECT_LIST, true);
    listTag = DOMUtils.getAttribute(listElement, FILTER_CONFIG_OBJECT_LIST_TAG, true);
  }
  
  public void processObjects() throws Exception {
    if(this.objProcessor != null) {
        Map<String, Object> map = new HashMap<String, Object>();
        List objects=null;
        try {
          objects = loader.getObjects();
          map.put(listTag, objects);
          this.objProcessor.processElement(map);
        } catch (FilterException e) {
        	Object obj = objects.get(0);
          log.error("Error processing loaded objects.Object:" + obj);
          throw e;
        } catch (Exception e) {
        	Object obj = objects.get(0);        	
          log.error("Error processing loaded objects.Object:" + obj);
          throw e;
        }
    }
  }
  
  public Object getInput(String filterName) {
    return this;
  }
  
  public Object getOutput(String _filterName, Object _output) throws ConnectException, UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Sets the Output for this filter.
   *  
   */
  public void setOutput(String sourceName, Object objProcessor) {
    this.objProcessor = (ObjectProcessor) objProcessor;
  }
  
  public void endDocument() throws SAXException {
	  handler.endDocument();
	  long time = (System.nanoTime() - start);
	  
		if (log.isTraceEnabled()) {
			Runtime rt = Runtime.getRuntime();
			log.trace("ABM.InvoiceLoaderFilter.AFT;Free:" + rt.freeMemory() + ";Total:" + rt.totalMemory() + ";Max:" + rt.maxMemory());
		}			  

	  log.info(i18n.getString("allFilters.endProcessing", this.getClass().getSimpleName(), this.filterName, String.valueOf(time/1000000)));
	  try {
		  this.processObjects();
	  } catch (Exception e) {
		  log.error(i18n.getString("invoiceLoader.endDoc.ex"), e);
		  throw new SAXException(e);
	  } finally {
			if (log.isTraceEnabled()) {
				Runtime rt = Runtime.getRuntime();
				log.trace("ABM.InvoiceLoaderFilter.AFT;Free:" + rt.freeMemory() + ";Total:" + rt.totalMemory() + ";Max:" + rt.maxMemory());
			}				  	
	  }
  }
  
  public void startDocument() throws SAXException {
	  log.info(i18n.getString("allFilters.startProcessing", this.getClass().getSimpleName(), this.filterName));
	  this.start = System.nanoTime();
	  handler.startDocument();
  }
  
  public void characters(char[] ch, int start, int length) throws SAXException {
    handler.characters(ch, start, length);
  }
  
  public void ignorableWhitespace(char[] ch, int start, int length)
  throws SAXException {
    handler.ignorableWhitespace(ch, start, length);
  }
  
  public void endPrefixMapping(String prefix) throws SAXException {
    handler.endPrefixMapping(prefix);
  }
  
  public void skippedEntity(String name) throws SAXException {
    handler.skippedEntity(name);
  }
  
  public void setDocumentLocator(Locator locator) {
    handler.setDocumentLocator(locator);
  }
  
  public void processingInstruction(String target, String data)
  throws SAXException {
    handler.processingInstruction(target, data);
  }
  
  public void startPrefixMapping(String prefix, String uri) throws SAXException {
    handler.startPrefixMapping(prefix, uri);
  }
  
  public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
    handler.endElement(namespaceURI, localName, qName);
  }
  
  public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
    handler.startElement(namespaceURI, localName, qName, atts);
  }

  public void commit() {
	  rollback();
  }
  
  public void rollback() {
      if (this.loader != null) { 
    	  loader.cleanup();
      } else {
    	  log.error("Invoice loader instance should not be NULL.");
      }
  };
}
