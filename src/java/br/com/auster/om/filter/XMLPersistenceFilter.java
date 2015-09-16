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
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

import org.w3c.dom.Element;

import com.thoughtworks.xstream.XStream;

import br.com.auster.dware.graph.ConnectException;
import br.com.auster.dware.graph.FilterException;


/**
 * @author mtengelm
 * @version $Id: XMLPersistenceFilter.java 185 2006-08-15 19:19:42Z mtengelm $
 */
public class XMLPersistenceFilter extends PersistenceFilter {

	private XStream	xs;

	/**
	 * @param _name
	 */
	public XMLPersistenceFilter(String _name) {
		super(_name);	
	}

	public void configure(Element config) throws FilterException {
		super.configure(config);
		xs = new XStream();
		xs.setMode(XStream.ID_REFERENCES);
	}
	
  public void setInput(String name, Object obj) throws ConnectException, UnsupportedOperationException {
    if (output instanceof WritableByteChannel) {
      output = Channels.newOutputStream((WritableByteChannel) output);
    }
    this.output =  (OutputStream) output;
	}

	/**
   * Sets the WritableByteChannel output for this filter.
   */
  public final void setOutput(String sinkName, Object output) {
    if (output instanceof WritableByteChannel) {
      output = Channels.newOutputStream((WritableByteChannel) output);
    }
    this.output =  (OutputStream) output;
  }
  
	public void printObject(Object _obj) throws IOException, FilterException {
		if (_obj == null) {
			return;
		}
		if (this.output == null) {
			throw new FilterException("Filter output set to null");
		}
		xs.toXML(_obj, output);
	}
}
