# Building the server #
Clone a copy of the [marmoset repository](http://code.google.com/p/marmoset/source/checkout) to get the latest version of the submit server. All libraries are contained in version control, so there is no need to download any dependencies. However, you will need to download the latest [GWT SDK](http://code.google.com/webtoolkit/download.html) for compiling the GWT portions of the server.

## Configuration Files ##
There are 3 sets of configuration files:
  * Build. These files contain properties that configure the build, but are not referenced by the server at runtime.
  * Web. These are the properties that configure the runtime behavior of the submit server.
  * Branding. These properties allow you to customize the branding of the submit server, such as institution name, logo, etc.

Each configuration has 2 files, for example `local.web.properties` and `web.properties`. The `{build,web,branding}.properties` files contain defaults where appropriate. The properties in the `local.*.properties` files overwrite those set in the non-local files, and also configure properties that have no reasonable default value, such as database access information. Before building and deploying a submit server, you'll need to create the local properties files and configure them as detailed below.

### `local.build.properties` ###
The submit server build is configured through the `local.build.properties` file. You must set the `gwt.sdk` property to the directory of the unzipped GWT SDK. You may set [other properties](BuildConfigProperties.md) to customize the build, but `gwt.sdk` is required.

### `local.web.properties` ###
Runtime configuration properties (DB access, authentication, etc) are set in a `local.web.properties` file. See WebProperties for details on the properties that you need to provide.

You'll need to choose an authentication mechanism. The default is [OpenID](OpenIdAuthentication.md), but you can also choose [LDAP](LdapAuthentication.md) as well. See their respective pages for details.

### `local.branding.properties` ###
You'll need to configure the branding properties for your submit server instance; see the defaults in `branding.properties` for examples.

# Build and Deploy #
The submit server must be deployed to a servlet container that implements the Servlet spec version 3.0 or later; a recent release of Apache Tomcat works very well. It is **highly recommended** that the servlet container be configured to run the submit server over https.

Build the submit server by invoking `ant` in the `SubmitServer2` directory. This will create the `dist/submitServer.war` file, which can be deployed to a servlet container.

# Database Initialization #
Create an empty SQL database with the [marmoset schema](http://marmoset.googlecode.com/git/SubmitServerModelClasses/marmoset-schema.sql).

You need to create an SQL user with data privileges (select, insert, update and delete), but no structure or administrative privileges (for security purposes, it is best if the db user used by the submit server has only the privileges it needs, but it shouldn't cause problems if it has more).

# Creating the first user #
Just open an web page for the system. You will be directed to page asking for your credentials. When
the system creates the first user, it will also create an administrative/superuser account for that user.