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
 * Created on Mai 12, 2006
 */
package br.com.auster.om.filter;

import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;

import br.com.auster.common.log.LogFactory;
import br.com.auster.common.util.I18n;
import br.com.auster.dware.graph.FilterException;


/**
 * This specialization of <code>HibernatePersistenceFilter</code> handles persistence of
 * 	resulting maps from <code>InvoiceRulesEngineFilter</code> class.
 * 
 * This is needed due to the so specialized way this filter stores the results from rule firing. 
 * 
 * @author framos
 * @version $Id$
 */
public class InvoiceRulesHibernatePersistenceFilter extends HibernatePersistenceFilter {

	
	
	private static final Logger log = LogFactory.getLogger(InvoiceRulesHibernatePersistenceFilter.class);
	private static final I18n i18n = I18n.getInstance(InvoiceRulesHibernatePersistenceFilter.class);
	
	
	
	public InvoiceRulesHibernatePersistenceFilter(String _name) {
		super(_name);
	}
	
	
	/**
	 * @see br.com.auster.dware.graph.ObjectProcessor#processElement(java.lang.Object)
	 */
	public void processElement(Object _objects) throws FilterException {
		Map resultsPerAccount = (Map) ((Map)_objects).get(this.listKey);
		if (resultsPerAccount == null) {
			log.warn(i18n.getString("invoiceHibernateOPFilter.noResultMap", this.listKey));
			return;
		}
		log.debug(i18n.getString("invoiceHibernateOPFilter.getResultMap", String.valueOf(resultsPerAccount.size())));
		// saving each account. Commit will also work BY ACCOUNT!!!
		for (Iterator it=resultsPerAccount.values().iterator(); it.hasNext(); ) {
			super.processElement(it.next());
		}
	}	
}
