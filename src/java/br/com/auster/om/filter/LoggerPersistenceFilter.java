/*
* Copyright (c) 2004-2005 Auster Solutions do Brasil. All Rights Reserved.
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
* Created on 11/08/2006
*/
//TODO Comment this Class
package br.com.auster.om.filter;

import java.io.IOException;

import org.apache.log4j.Logger;

import br.com.auster.dware.graph.FilterException;


/**
 * @author mtengelm
 * @version $Id: LoggerPersistenceFilter.java 185 2006-08-15 19:19:42Z mtengelm $
 */
public class LoggerPersistenceFilter extends PersistenceFilter {
	
	private static Logger log = Logger.getLogger(LoggerPersistenceFilter.class);
	/**
	 * @param _name
	 */
	public LoggerPersistenceFilter(String _name) {
		super(_name);	
	}

	public void printObject(Object _obj) throws IOException, FilterException {
		if (_obj == null) {
			return;
		}
		//Fatal os to make sure that it is gonna be printed.
		log.fatal(_obj.toString() );
	}
}
