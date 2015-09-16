/*
 * Copyright (c) 2004 Auster Solutions. All Rights Reserved.
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
 * Created on 09/08/2006
 */
package br.com.auster.om.filter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import br.com.auster.common.util.I18n;
import br.com.auster.common.xml.DOMUtils;
import br.com.auster.dware.graph.ConnectException;
import br.com.auster.dware.graph.DefaultFilter;
import br.com.auster.dware.graph.FilterException;
import br.com.auster.dware.graph.ObjectProcessor;

/**
 * @author framos
 * @version $Id: PersistenceFilter.java 472 2007-10-12 20:53:11Z framos $
 */
public abstract class PersistenceFilter extends DefaultFilter implements ObjectProcessor {


	private static final I18n i18n = I18n.getInstance(PersistenceFilter.class);
	private static final Logger log = Logger.getLogger(PersistenceFilter.class);


	public static final String	INPUTMAP_LISTKEY_ATTR	= "input-list-tag";

	protected OutputStream output;
	protected String listKey;

	public PersistenceFilter(String _name) {
		super(_name);
	}

	/**
	 * @see br.com.auster.dware.graph.DefaultFilter#configure(org.w3c.dom.Element)
	 */
	public void configure(Element _configuration) throws FilterException {
		this.listKey = DOMUtils.getAttribute(_configuration, INPUTMAP_LISTKEY_ATTR, false);
	}

	/**
	 * @see br.com.auster.dware.graph.ObjectProcessor#processElement(java.lang.Object)
	 */
	public void processElement(Object _objects) throws FilterException {
		log.info(i18n.getString("allFilters.startProcessing", this.getClass().getSimpleName(), this.filterName));
		long start = System.currentTimeMillis();
		try {
			if ((_objects instanceof Map) && (this.listKey != null)) {
				printObject(((Map) _objects).get(this.listKey));
			} else {
				printObject(_objects);
			}
		} catch (IOException ioe) {
			throw new FilterException(ioe);
		}
		long time = (System.currentTimeMillis() - start);
		log.info(i18n.getString("allFilters.endProcessing", this.getClass().getSimpleName(), this.filterName, String.valueOf(time)));
	}

	/**
	 * @see br.com.auster.dware.filter.DefaultFilter#getInput(java.lang.String)
	 */
	public Object getInput(String filterName) throws ConnectException, UnsupportedOperationException {
		return this;
	}

	/**
	 * @see br.com.auster.dware.filter.DefaultFilter#setOutput(java.lang.String,
	 *      java.lang.Object)
	 */
	public void setOutput(String _name, Object _output) throws ConnectException, UnsupportedOperationException {
		this.output = (OutputStream) _output;
	}

	public abstract void printObject(Object _obj) throws IOException, FilterException;


}
