/*
 * Copyright (c) 2004-2007 Auster Solutions do Brasil. All Rights Reserved.
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
package br.com.auster.om.filter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import br.com.auster.common.xml.DOMUtils;
import br.com.auster.dware.graph.FilterException;

import com.thoughtworks.xstream.XStream;

/**
 * @author framos
 * @version $Id$
 */
public class XStreamSerializerFilter extends PersistenceFilter {


	private static final Logger log = Logger.getLogger(XStreamSerializerFilter.class);


	public static final String IGNORE_EMPTY_FLAG = "ignore-empty";



	private XStream xs;
	private Writer outputWriter;
	private boolean ignoreEmpty;

	/**
	 * @param _name
	 */
	public XStreamSerializerFilter(String _name) {
		super(_name);
	}

	public void configure(Element config) throws FilterException {
		super.configure(config);

		this.ignoreEmpty = DOMUtils.getBooleanAttribute(config, IGNORE_EMPTY_FLAG, false);
		log.debug("Ignore empty flagset to " + this.ignoreEmpty);

		xs = new XStream();
		xs.setMode(XStream.NO_REFERENCES);
		xs.useAttributeFor(String.class);

	}


	/**
	 * Sets the WritableByteChannel output for this filter.
	 */
	public final void setOutput(String sinkName, Object output) {
		if (output instanceof WritableByteChannel) {
			this.output = Channels.newOutputStream((WritableByteChannel) output);
			log.debug("Output for XStream set as WritableByteChannel");
		} else if (output instanceof OutputStream) {
			this.output = (OutputStream) output;
			log.debug("Output for XStream set as OutputStream");
		} else if (output instanceof Writer) {
			this.outputWriter = (Writer) output;
			log.debug("Output for XStream set as Writer");
		} else {
			throw new IllegalArgumentException("Cannot connect XStream filter without a stream or writer output" + output.getClass());
		}
	}



	public void printObject(Object _obj) throws IOException, FilterException {
		try {
			if (_obj == null) {
				return;
			}
			// checking ignore empty flag
			if ((_obj instanceof Collection) && (this.ignoreEmpty) && (((Collection)_obj).size() == 0)) {
				log.info("Since ignore flag is true and the input Collection is empty, we will not forward any information.");
				return;
			}

			if (this.output != null) {
				xs.toXML(_obj, output);
			} else if (this.outputWriter != null) {
				xs.toXML(_obj, outputWriter);
			} else {
				throw new FilterException("Filter output set to null");
			}
		} finally {
			if (this.output != null) { this.output.close(); }
			if (this.outputWriter != null) { this.outputWriter.close(); }
		}
	}
}
