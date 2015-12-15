The `test.properties` file must be included in every test setup, regardless of language. The build server reads it to determine how to build and test a given submission.

If a property's default value is listed as "required," it must be specified in the `test.properties` file. If it is listed as "optional," it need not be specified, but has no default value: a missing value is simply ignored.

| **Property Key** | **Default** | **Description**|
|:-----------------|:------------|:|
| `build.language` | Required    | The language used in the project. Valid values are "java" and "c". |
| `test.class.public`| Optional    | A comma-separated list of public test cases to run. In a Java project, the test cases listed should be class names; in a C project they should be a list of executables. In both cases, the test cases must already be compiled, since the build server will compile only submission code. Note that all of the test.class.**properties are optional, but at least one test class must be specified for any tests to be run.**|
| `test.class.release` | Optional    | A comma-separated list of release test cases. See the notes for `test.class.public`. |
| `test.class.secret` | Optional    | A comma-separated list of release test cases. See the notes for `test.class.public`. |
| `test.timeout`   | 60          | Maximum time, in seconds, that a single test may run. |
| `test.output.maxBytes` | 2<sup>20</sup> | Maximum number of bytes that a test may output. Any additional output past this limit is simply ignored. |
| `build.sourceVersion` | 1.6         | Java source version expected in the submission code. Applies only to Java projects. |
| `test.runInInstructorDir` | `false`     | TODO(rwsims): What is this for? |
| `build.make.command` | `/usr/bin/make` | Make command for compiling C projects. |
| `build.make.file` | Optional    | Name of the instructor-provided makefile. |
| `build.student.make.file` | Optional    | Name of the student-provided makefile. |
| `display.source.extensions` | Optional    | List of additional extensions that should be considered source files for display. Many "obvious" source file extensions, such as `.java`, `.c`, `.xml`, etc. are already included; this is only needed if source files are mistakenly not being displayed. |
| `test.performCodeCoverage` | false       | Whether to run code coverage metrics on test cases. |