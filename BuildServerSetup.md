# Getting the Configuration #
You'll need to have a course set up for the build server to build; see CreatingCourses. Log in as the instructor, and navigate to the Instructor View for that course (`view/instructor/course.jsp`). Under "Server Status," you'll see a list of any currently-active build servers, and link for creating a config file for running a build server. Save the link as "`config.properties`."

# Starting the Server #
  * TODO: Need to document how people get their hands on a build server jar file.
  * TODO: Need to document untrusted/trusted user setup.

Put the `buildserver.jar` and `config.properties` files in the directory where you want the build server to run. Execute the build server by running the following command:
```
java -jar buildserver.jar config.properties
```

See the [build server properties](BuildServerConfig.md) reference for other options that can be specified in `config.properties`.

The build server daemon will connect to the submit server specified in the config file and request work. By default, logs go to
```
${BUILDSERVER_DIRECTORY}/log/buildserver.log
```