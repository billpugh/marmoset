package edu.umd.cs.submitServer.tags;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import org.apache.commons.lang3.StringEscapeUtils;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

/** Tag handler to load a property from the {@code /web.properties} file. The file must be at the root of the classpath.
 * 
 * @author rwsims
 *
 */
public class WebPropertyTag extends SimpleTagSupport {
	private static final Properties webProperties = new Properties();
	
	static {
		try {
	    InputStream propStream = WebPropertyTag.class.getResourceAsStream("/web.properties");
	    Preconditions.checkNotNull(propStream, "Web properties not found");
			webProperties.load(propStream);
    } catch (IOException e) {
	    throw new RuntimeException(e);
    }
	}
	
	private boolean safeHtml = false;
	private String key;
	
	public void setKey(String key) {
	  this.key = key;
  }
	
	public void setSafeHtml(boolean safeHtml) {
	  this.safeHtml = safeHtml;
  }
	
	@Override
	public void doTag() throws JspException, IOException {
	  Preconditions.checkState(!Strings.isNullOrEmpty(key), "Property key was not set");
	  String value = webProperties.getProperty(key);
	  if (!safeHtml) {
	  	value = StringEscapeUtils.escapeHtml4(value);
	  }
	  getJspContext().getOut().write(value);
	}
}
