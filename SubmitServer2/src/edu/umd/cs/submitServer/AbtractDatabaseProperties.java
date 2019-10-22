package edu.umd.cs.submitServer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import javax.servlet.ServletContext;

public abstract class AbtractDatabaseProperties {
  private static final WebConfigProperties webProperties = WebConfigProperties.get();

  protected final String databaseURL;
  protected final String databaseUser;
  protected final String databasePassword;
  protected final String contextPath;

  /**
   * @param databaseURL
   * @param databaseName
   * @param databaseOptions
   * @param databaseUser
   * @param databasePassword
   * @param databaseDriver
   * @throws ClassNotFoundException
   */
  public AbtractDatabaseProperties(ServletContext servletContext,
      String databaseURL, String databaseName,
      String databaseOptions, String databaseUser,
      String databasePassword, String databaseDriver)  {

    contextPath = servletContext.getContextPath();
    String driver = webProperties.getRequiredProperty(databaseDriver);
    try {
      Class.forName(driver);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("Unable to load sql driver " + driver);
    }

    String url = webProperties.getRequiredProperty(databaseURL);
    String name =  webProperties.getProperty(databaseName);
    String options = webProperties.getRequiredProperty(databaseOptions);


    if ("ssl".equals(options))
      options = "verifyServerCertificate=false" + "&useSSL=true" + "&requireSSL=true";
    if (options.isEmpty())
      options = "characterEncoding=utf8&characterSetResults=utf8";
    else
      options += "&characterEncoding=utf8&characterSetResults=utf8";
    if (name != null) {
      // allow database name in URL to be overridden
      int index = url.lastIndexOf('/');
      if (index >= 0)
        url = url.substring(0, index + 1) + databaseName;
      else
        url = url + "/" + databaseName;
    }
    if (options.length() > 0)
      url = url + "?" + options;

    this.databaseURL = url;
    this.databaseUser =  webProperties.getRequiredProperty(databaseUser);
    this.databasePassword =  webProperties.getRequiredProperty(databasePassword);
  }
  
  public String getContextPath() {
    return contextPath;
  }


  public Connection getConnection() throws SQLException {
    if (databaseURL == null)
      throw new IllegalArgumentException("databaseServer not set");
    if (databaseUser == null)
      throw new IllegalArgumentException("databaseUser not set");
    if (databasePassword == null)
      throw new IllegalArgumentException("databasePassword not set");

    Connection conn;
    try {
      conn = DriverManager.getConnection(databaseURL, databaseUser,
          databasePassword);
    } catch (SQLException e) {
      throw new SQLException("Failed to connect to " + databaseURL, e);
    }
    return conn;
  }

  public void releaseConnection(Connection conn) throws SQLException {
    if (conn != null)
      conn.close();
  }

}