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
 * Created on 22/12/2006
 */
package br.com.auster.om.filter.request;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import br.com.auster.dware.request.file.FileRequest;

import junit.framework.TestCase;

/**
 * @author mtengelm
 *
 */
public class TestBillcheckoutRequestWrapper extends TestCase {

	private BufferedWriter br;
	private File file;
	private String fname= null;

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		fname = "dware-test.txt";
		file = new File(fname);
		br = new BufferedWriter(new FileWriter(file));
		assertNotNull(file);
		assertNotNull("Buffered Writter SHOULD NOT be null" , br);
		br.write("Linha01");
		br.write("\n");
		br.write("Linha02");
		br.write("\n");
		br.flush();
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		file = null;
		br.close();
		br = null;
	}

	/**
	 * Test method for {@link br.com.auster.om.filter.request.BillcheckoutRequestWrapper#BillcheckoutRequestWrapper()}.
	 */
	public void testBillcheckoutRequestWrapper() {
		FileRequest request = new FileRequest(file);		
		BillcheckoutRequestWrapper req = new BillcheckoutRequestWrapper(request);
		assertNotNull(req);
		assertNotNull(req.getRequest());
	}

	/**
	 * Test method for {@link br.com.auster.om.filter.request.BillcheckoutRequestWrapper#BillcheckoutRequestWrapper(br.com.auster.dware.graph.Request)}.
	 */
	public void testBillcheckoutRequestWrapperRequest() {
		BillcheckoutRequestWrapper req = new BillcheckoutRequestWrapper();
		assertNotNull(req);
		assertNull(req.getRequest());
	}

	/**
	 * Test method for {@link br.com.auster.om.filter.request.BillcheckoutRequestWrapper#getAttributes()}.
	 */
	public void testGetAttributes() {
		FileRequest request = new FileRequest(file);		
		BillcheckoutRequestWrapper req = new BillcheckoutRequestWrapper(request);
		assertNotNull(req);
		assertNotNull(req.getRequest());
		assertEquals(2, req.getAttributes().size());
		
		request.setTransactionId("My Transaction ID");
		assertEquals(3, req.getAttributes().size());
	}

}
