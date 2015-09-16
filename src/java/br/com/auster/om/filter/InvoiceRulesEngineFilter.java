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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import br.com.auster.common.rules.RulesEngineProcessor;
import br.com.auster.common.util.I18n;
import br.com.auster.common.xml.DOMUtils;
import br.com.auster.dware.graph.ConnectException;
import br.com.auster.dware.graph.DefaultFilter;
import br.com.auster.dware.graph.FilterException;
import br.com.auster.dware.graph.ObjectProcessor;
import br.com.auster.dware.graph.Request;
import br.com.auster.om.filter.request.BillcheckoutRequestWrapper;
import br.com.auster.om.invoice.InvoiceModelObject;
import br.com.auster.om.invoice.rules.InvoiceAssertionWorker;

/**
 * <p>
 * <b>Title:</b> InvoiceRulesEngineFilter
 * </p>
 * <p>
 * <b>Description:</b> A Rules Engine filter that instantiates a rules engine,
 * asserts Invoice OM objects and fire rules for them
 * </p>
 * <p>
 * <b>Copyright:</b> Copyright (c) 2006
 * </p>
 * <p>
 * <b>Company:</b> Auster Solutions
 * </p>
 *
 * @author etirelli
 * @version $Id: InvoiceRulesEngineFilter.java 565 2008-07-30 22:53:38Z framos $
 */
public class InvoiceRulesEngineFilter extends DefaultFilter implements ObjectProcessor {

	// ---------------------------
	// Class constants
	// ---------------------------

	private static final I18n i18n = I18n.getInstance(InvoiceRulesEngineFilter.class);
	private static final Logger log = Logger.getLogger(InvoiceRulesEngineFilter.class);

	public static final String	 FILTER_CONFIG_ENGINE_PLUGIN	     = "rules-engine";
	public static final String	 FILTER_CONFIG_ENGINE_PLUGIN_CLASS	= "class-name";
	public static final String	 CONFIG_INPUT_LIST	               = "input-list-tag";
	public static final String	 CONFIG_RESULTS_TAG	               = "results-map-tag";
	public static final String	 CONFIG_NAME_ATTR	                 = "name";

	public static final String	 ASSERTION_ELMT	                   = "assertion";
	public static final String	 ASSERTION_TYPE_ATTR	             = "type";
	public static final String	 ASSERTION_TYPE_REFLECTION	       = "reflection";
	public static final String	 ASSERTION_TYPE_USAGE	             = "usage";
	public static final String	 ASSERTION_TYPE_SIMPLE             = "simple";

	public static final String	 FILTER_CONFIG_GLOBALSLIST_ELEMENT	= "globals-list";
	public static final String	 FILTER_CONFIG_GLOBAL_ELEMENT	     = "global";
	public static final String	 FILTER_CONFIG_GLOBAL_NAME	       = "name";
	public static final String	 FILTER_CONFIG_GLOBAL_CLASS	       = "class-name";

	// ---------------------------
	// Instance variables
	// ---------------------------

	protected RulesEngineProcessor	engine;
	protected String	             inputTag;
	protected String	             resultsTag;
	protected ObjectProcessor	     objProcessor;
	protected Map<String, Object>	 globals;

	protected String assertionType = ASSERTION_TYPE_REFLECTION;

	protected Request req;

	// ---------------------------
	// Constructor
	// ---------------------------

	public InvoiceRulesEngineFilter(String _name) {
		super(_name);
	}

	// ---------------------------
	// Public methods
	// ---------------------------

	/**
	 * <P>
	 * Walks through the <code>_configuration</code> parameter looking for the
	 * invoice rules engine plugin class.
	 * </P>
	 *
	 * @param _configuration
	 *          the root configuration DOM element
	 *
	 * @exception FilterException
	 *              if anything when wrong while reading the configuration
	 */
	public void configure(Element _configuration) {
		log.debug("Configuring InvoiceRulesEngineFilter");
		Element engineElement = DOMUtils.getElement(_configuration, FILTER_CONFIG_ENGINE_PLUGIN, true);
		String engineKlass = DOMUtils.getAttribute(engineElement, FILTER_CONFIG_ENGINE_PLUGIN_CLASS,
		    true);

		// Loading Facade List
		this.globals = new HashMap<String, Object>();
		Element globalsListElmt = DOMUtils.getElement(_configuration,
		    FILTER_CONFIG_GLOBALSLIST_ELEMENT, false);
		if (globalsListElmt != null) {
			NodeList globalsList = DOMUtils.getElements(globalsListElmt, FILTER_CONFIG_GLOBAL_ELEMENT);
			for (int i = 0; globalsList.getLength() > i; i++) {
				Element currentGlobal = (Element) globalsList.item(i);
				String klassName = DOMUtils.getAttribute(currentGlobal, FILTER_CONFIG_GLOBAL_CLASS, true);
				String name = DOMUtils.getAttribute(currentGlobal, FILTER_CONFIG_GLOBAL_NAME, true);
				try {
					Class klass = Class.forName(klassName);
					Object globalInstance = klass.newInstance();
					log.info("Global Parameter Class [" + klass.getCanonicalName()
					    + "] successfully instantiated.");
					try {
						Method method = klass.getMethod("configure", new Class[] { Element.class });
						method.invoke(globalInstance, new Object[] { currentGlobal });
					} catch (NoSuchMethodException nsme) {
						log.warn("Global " + name + " doesnot have a configuration method.");
					} catch (IllegalArgumentException e) {
						throw new RuntimeException(e);
					} catch (InvocationTargetException e) {
						throw new RuntimeException(e);
					}
					this.globals.put(DOMUtils.getAttribute(currentGlobal, FILTER_CONFIG_GLOBAL_NAME, true),
					    globalInstance);
					log.debug("Created instance of class " + klassName + " named as " + name);
				} catch (ClassNotFoundException e) {
					log.warn("Global " + name + " with class " + klassName
					    + " not found. Ignoring it for now.");
				} catch (InstantiationException e) {
					throw new RuntimeException(e);
				} catch (IllegalAccessException e) {
					throw new RuntimeException(e);
				}

			}
		}
		try {
//			Object o = Class.forName(engineKlass).newInstance();
//			this.engine = (RulesEngineProcessor) o;

			Class[] c = { String.class };
		    Object[] o = { this.filterName };
		    this.engine = (RulesEngineProcessor) Class.forName(engineKlass).getConstructor(c).newInstance(o);

			this.engine.configure(engineElement);
			this.engine.init(this.globals);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		Element inputElem = DOMUtils.getElement(_configuration, CONFIG_INPUT_LIST, true);
		inputTag = DOMUtils.getAttribute(inputElem, CONFIG_NAME_ATTR, true);
		Element resultsElem = DOMUtils.getElement(_configuration, CONFIG_RESULTS_TAG, true);
		resultsTag = DOMUtils.getAttribute(resultsElem, CONFIG_NAME_ATTR, true);

		// Handles Assertion type configuration.
		// If not present or wrong assumes by reflection.
		Element assertion = DOMUtils.getElement(_configuration, ASSERTION_ELMT, false);
		if (assertion != null) {
			String assertType = DOMUtils.getAttribute(assertion, ASSERTION_TYPE_ATTR, false);
			if ((null == assertType) || (assertType.length() == 0) || (assertType.equalsIgnoreCase(ASSERTION_TYPE_REFLECTION)) ) {
				this.assertionType = ASSERTION_TYPE_REFLECTION;
			} else if (assertType.equalsIgnoreCase(ASSERTION_TYPE_USAGE)) {
				this.assertionType = ASSERTION_TYPE_USAGE;
			} else if (assertType.equalsIgnoreCase(ASSERTION_TYPE_SIMPLE)) {
				this.assertionType = ASSERTION_TYPE_SIMPLE;
			} else {
				log.warn("Unreconized assertion type:" + assertType + " assuming:" + ASSERTION_TYPE_REFLECTION);
				this.assertionType = ASSERTION_TYPE_REFLECTION;
			}
		}
		log.info("Assertion type is " + this.assertionType);
	}

	protected void processRules(Map<String, Object> inputMap) throws Exception {
		List objects = (List) inputMap.get(inputTag);
		inputMap.put(resultsTag, Collections.EMPTY_LIST);
		Map appData = (this.globals != null) ? this.globals : new HashMap();
		for (Iterator i = objects.iterator(); i.hasNext();) {
			this.engine.prepare(appData);
			InvoiceModelObject account = (InvoiceModelObject) i.next();
			long start = System.currentTimeMillis();
			log.debug("ProcessingRules for account [" + account + "]");

			if (this.assertionType.equals(ASSERTION_TYPE_SIMPLE)) {
				InvoiceAssertionWorker.assertSimpleUsage(engine, account);
			} else if (this.assertionType.equals(ASSERTION_TYPE_REFLECTION)) {
				InvoiceAssertionWorker.assertObjectsByReflection(engine, account);
			} else {
				InvoiceAssertionWorker.assertUsageObjects(engine, account);
			}

			//Asserts the DWARE request as a FACT
			this.engine.assertFact(new BillcheckoutRequestWrapper( this.req) );

			this.engine.fireRules();
			List results = this.engine.getResults();
			log.info("Filter named " + this.filterName + "processed [" + results.size() + "] results for account [" + account+ "]");
			// log messages splitted to keep time control messages in the same format
			long time = (System.currentTimeMillis() - start);
			log.info(i18n.getString("allFilters.endProcessing", this.getClass().getSimpleName(), this.filterName, String.valueOf(time)));
			inputMap.put(resultsTag, results);
		}
		this.objProcessor.processElement(inputMap);
	}

	public Object getInput(String filterName) throws ConnectException, UnsupportedOperationException {
		return this;
	}

	public Object getOutput(String _filterName, Object _output) throws ConnectException,
	    UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	/**
	 * Sets the Output for this filter.
	 *
	 */
	public void setOutput(String sourceName, Object objProcessor) {
		this.objProcessor = (ObjectProcessor) objProcessor;
	}

	/* (non-Javadoc)
   * @see br.com.auster.dware.graph.DefaultFilter#prepare(br.com.auster.dware.graph.Request)
   */
  @Override
  public void prepare(Request req) throws FilterException {
		this.req = req;
  }

	/**
	 * @inheritDoc
	 */
	public void processElement(Object map) throws FilterException {
		log.info(i18n.getString("allFilters.startProcessing", this.getClass().getSimpleName(), this.filterName));
		if (this.objProcessor != null) {
			try {
				Map<String, Object> inputMap = (Map<String, Object>) map;
				this.processRules(inputMap);
			} catch (Exception ex) {
				log.error("Error processing rules", ex);
				throw new FilterException("Error processing rules", ex);
			}
		}
	}

  /**
   * @inheritDoc
   */
  public void commit() {
    rollback();
  }

  /**
   * @inheritDoc
   */
  public void rollback() {
    try {
      if (this.engine != null) {
        this.engine.clear();
      }
    } catch (Exception e) {
      log.warn("Problemas during cleanup: " + e.getMessage());
    }
  }



}
