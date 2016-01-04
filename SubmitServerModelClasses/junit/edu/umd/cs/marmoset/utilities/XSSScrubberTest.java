package edu.umd.cs.marmoset.utilities;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

import junit.framework.TestCase;

public class XSSScrubberTest extends TestCase {


	public void testEncodePath() throws UnsupportedEncodingException {
		checkEncodePath("/foo/bar");
		checkEncodePath("foo");
	}
	
	private void checkEncodePath(String path) throws UnsupportedEncodingException {
		checkEncoding(path, XSSScrubber.urlEncodePath(path));
		String alreadyEncoded = URLEncoder.encode(path, "UTF-8");
		checkEncoding(path, XSSScrubber.urlEncodePath(alreadyEncoded));
	}
	
	private void checkEncoding(String path, String encoded) throws UnsupportedEncodingException {
		assertEquals(path, URLDecoder.decode(encoded, "UTF-8"));
		
	}

}
