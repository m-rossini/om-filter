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
 * Created on Mai 11, 2006
 */
package br.com.auster.om.filter;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import br.com.auster.common.io.IOUtils;
import br.com.auster.common.log.LogFactory;
import br.com.auster.common.util.I18n;
import br.com.auster.common.xml.DOMUtils;
import br.com.auster.dware.graph.ConnectException;
import br.com.auster.dware.graph.DefaultFilter;
import br.com.auster.dware.graph.FilterException;
import br.com.auster.dware.graph.ObjectProcessor;
import br.com.auster.om.filter.hibernate.HibernateEvicter;

/**
 * This class allows persistence of objects using a hibernate mapping. This
 * hibernate mapping file should be provided in configuration time.
 * 
 * When processing the incoming object parameter, the filter will check its
 * type. If its: a) a <code>List</code>, then it will iterate and execute
 * <code>saveOrUpdate()</code> for each element of the list; b) a
 * <code>Map</code>, then a key name (at configuration time) should be
 * provided. If the object mapped by such key is an instance of
 * <code>List</code> then it will be iterated and each element saved (or
 * updated, as describe above). Else, the object itself will be saved. c) if
 * none of the above where true, then the object itself will be saved (or
 * updated)
 * 
 * Due to performance issues, the configuration allows setting a number of
 * <code>saveOrUpdate()</code> executions before really committing the data to
 * the database. If not set, this value is left as <code>1</code>, meaning
 * each time an object is saved, it will be committed to the database.
 * 
 * As a security option, the hibernate configuration file can be encrypted. If
 * this attribute is not specified, then the filter will assume its not securely
 * encrypted.
 * 
 * @author framos
 * @version $Id$
 */
public class HibernatePersistenceFilter extends DefaultFilter implements
		ObjectProcessor {

	public static final String	INPUTMAP_LISTKEY_ATTR					= "input-list-tag";
	public static final String	HIBERNATE_CONFIGURATION_ELEM	= "hibernate";
	public static final String	HIBERNATE_FILE_ATTR						= "file-name";
	public static final String	HIBERNATE_COMMIT_ATTR					= "commit-count";
	public static final String	HIBERNATE_DRY_RUN							= "dry-run";
	public static final String	HIBERNATE_ENCRYPTED_ATTR			= "encrypted";
	public static final String	HIBERNATE_EVICTER_CLASS				= "evicter";

	private static final Logger	log														= LogFactory
																																.getLogger(HibernatePersistenceFilter.class);
	private static final I18n		i18n													= I18n
																																.getInstance(HibernatePersistenceFilter.class);

	protected String						listKey;
	protected int								commitCount										= 1;
	protected boolean						dryRun												= false;
	protected SessionFactory		factory;
	protected ObjectProcessor		objProcessor;

	private Connection					connection;

	protected HibernateEvicter	evicter;

	public HibernatePersistenceFilter(String _name) {
		super(_name);
	}

	public Session openSession() {
		return (this.factory == null ? null : this.factory.openSession());
	}

	public SessionFactory getSessionFactory() {
		return this.factory;
	}

	/**
	 * @see br.com.auster.dware.graph.DefaultFilter#configure(org.w3c.dom.Element)
	 */
	public void configure(Element _configuration) throws FilterException {
		this.listKey = DOMUtils.getAttribute(_configuration, INPUTMAP_LISTKEY_ATTR,
				true);
		log
				.debug(i18n
						.getString("hibernatePFilter.configureListKey", this.listKey));
		// getting hibernate configuration
		Element hbmCfg = DOMUtils.getElement(_configuration,
				HIBERNATE_CONFIGURATION_ELEM, true);
		String hbfile = DOMUtils.getAttribute(hbmCfg, HIBERNATE_FILE_ATTR, true);
		boolean encrypted = DOMUtils.getBooleanAttribute(hbmCfg,
				HIBERNATE_ENCRYPTED_ATTR, false);
		log.info(i18n.getString("hibernatePFilter.configureFile", hbfile, String
				.valueOf(encrypted)));
		this.commitCount = DOMUtils.getIntAttribute(hbmCfg, HIBERNATE_COMMIT_ATTR,
				false);
		// defaults to 1 in case its not specified
		if (this.commitCount < 1) {
			this.commitCount = 1;
		}
		log.debug(i18n.getString("hibernatePFilter.configureCommit", String
				.valueOf(this.commitCount)));
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		dbf.setValidating(false);
		try {
			Document doc = dbf.newDocumentBuilder().parse(
					IOUtils.openFileForRead(hbfile, encrypted));
			Configuration cfg = new Configuration();
			cfg.configure(doc);
			factory = cfg.buildSessionFactory();
		} catch (Exception e) {
			throw new FilterException(i18n.getString("hibernatePFilter.configureEx"),
					e);
		}
		// loading evicter class, if defined
		String evicterKlass = DOMUtils.getAttribute(hbmCfg,
				HIBERNATE_EVICTER_CLASS, false);
		if ((evicterKlass != null) && (evicterKlass.trim().length() > 0)) {
			try {
				this.evicter = (HibernateEvicter) Class.forName(evicterKlass)
						.newInstance();
			} catch (Exception e) {
				throw new FilterException(i18n
						.getString("hibernatePFilter.configureEvictEx"), e);
			}
		}
		// getting dry run configuration
		this.dryRun = DOMUtils
				.getBooleanAttribute(hbmCfg, HIBERNATE_DRY_RUN, false);
		log.info(i18n.getString("hibernatePFilter.dryRun", String
				.valueOf(this.dryRun)));
		log.info(i18n.getString("hibernatePFilter.configureOK"));
	}

	/**
	 * @see br.com.auster.dware.graph.ObjectProcessor#processElement(java.lang.Object)
	 */
	public void processElement(Object _objects) throws FilterException {
		// getting object list from Map, if this is the case
		Session session = null;
		long start = System.currentTimeMillis();
		log.info(i18n.getString("allFilters.startProcessing", this.getClass()
				.getSimpleName(), this.filterName));
		try {
			session = this.openSession();
			log.debug(i18n.getString("hibernatePFilter.sessionOpened"));
			if ((_objects instanceof Map)
					&& ((this.listKey != null) && (this.listKey.trim().length() > 0))) {
				log.debug(i18n.getString("hibernatePFilter.processMap"));
				saveObject(session, ((Map) _objects).get(this.listKey));
			} else {
				log.debug(i18n.getString("hibernatePFilter.processDirect"));
				saveObject(session, _objects);
			}
		} catch (HibernateException he) {
			throw new FilterException(he);
		} catch (SQLException sqle) {
			throw new FilterException(sqle);
		} finally {
			if (session != null) {
				try {
					runCommit(session);
					session.close();
				} catch (Exception e) {
					throw new FilterException(e);
				}
			}
			log.debug(i18n.getString("hibernatePFilter.sessionReleased"));
		}
		long time = (System.currentTimeMillis() - start);
		log.info(i18n.getString("allFilters.endProcessing", this.getClass()
				.getSimpleName(), this.filterName, String.valueOf(time)));
		
		if (this.objProcessor != null) {
			log.debug(i18n.getString("allFilters.hasNextFilter"));
			this.objProcessor.processElement(_objects);
		} else {
			log.debug(i18n.getString("allFilters.noNextFilter"));
		}
	}

	protected void saveObject(Session _session, Object _object)
			throws HibernateException, SQLException {
		// logging object classname and content
		log.debug(i18n.getString("hibernatePFilter.analyseObject", _object
				.getClass().getName()));
		log.debug(i18n.getString("hibernatePFilter.dumpObject", _object));
		int counter = 0;
		if (_object instanceof Collection) {
			for (Iterator it = ((List) _object).iterator(); it.hasNext();) {
				Object persistedObject = it.next();
				runSave(_session, persistedObject, true, false);
				if (this.evicter != null) {
					this.evicter.evict(_session, persistedObject);
				}
				counter++;
				// commit interval check
				if (counter == this.commitCount) {
					log.debug(i18n.getString("hibernatePFilter.partialCommitExecuted",
							String.valueOf(counter)));
					runCommit(_session);
					counter = 0;
				}
			}
		} else {
			runSave(_session, _object, true, false);
			counter++;
		}
	}

	/**
	 * @see br.com.auster.dware.filter.DefaultFilter#getInput(java.lang.String)
	 */
	public Object getInput(String filterName) throws ConnectException,
			UnsupportedOperationException {
		return this;
	}

	/**
	 * Sets the Output for this filter.
	 * 
	 */
	public void setOutput(String sourceName, Object objProcessor) {
		this.objProcessor = (ObjectProcessor) objProcessor;
	}

	protected final void runCommit(Session _session) throws HibernateException,
			SQLException {
		if (this.dryRun) {
			log.warn(i18n.getString("hibernatePFilter.commit.dryRunExecuted"));
			return;
		}
		_session.flush();
		_session.connection().commit();
	}

	protected final void runSave(Session _session, Object _toPersist,
			boolean _isUpdatable, boolean _commit) throws HibernateException,
			SQLException {
		if (this.dryRun) {
			log.debug(i18n.getString("hibernatePFilter.save.dryRunExecuted"));
			return;
		}
		if (_isUpdatable) {
			_session.saveOrUpdate(_toPersist);
		} else {
			_session.save(_toPersist);
		}
		if (_commit) {
			this.runCommit(_session);
		}
	}

	@Override
	public void commit() {
		super.commit();
		if (this.connection != null) {
			try {
				this.connection.close();
			} catch (SQLException sqle) {
				log.error(i18n.getString("hibernatePFilter.commit.sqlEx"), sqle);
			}
		}
	}

	@Override
	public void rollback() {
		super.rollback();
		if (this.connection != null) {
			try {
				this.connection.close();
			} catch (SQLException sqle) {
				log.error(i18n.getString("hibernatePFilter.commit.sqlEx"), sqle);
			}
		}
	}
}
