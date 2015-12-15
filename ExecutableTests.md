# Introduction #

This describes the testing framework used by Marmoset. In particular, the non-Java testing framework has been significantly extended.

The framework supports some older mechanisms and property names, intended for backwards compatibility. These are still supported.


# Details #

There are three different test frameworks: Java/jUnit, Script, and Make. The default is Java/jUnit. Specify the test framework using:

`test.framework = `_name_

Any name other than `java`, `junit` or `script` is treated as `make` for backwards compatibility.

The `script` and `make` frameworks run tests identically. The only difference is that with the `make` framework, the testing framework will run `make` to compile/build test code before the tests are executed.

# using make to build/compile tests #

If you are using the make framework, you may define the following properties.

| **Property key** | **Default** | **Description** |
|:-----------------|:------------|:----------------|
| `build.make.command` | `/usr/bin/make` | Make command for compiling C projects. |
| `build.make.file` | `Makefile`  | Name of the instructor-provided makefile. |
| `build.student.make.file` | _optional_  | Name of the student-provided makefile; not made if not provided |

# Running executable tests #

For any test case, you specify the command to be run (e.g., python p1.py), using Runtime.exec. You can also specify the standard input for the test case. This can be specified as a String, or by providing a file, the contents of while is provided as input.

With an executable test, you can either just check to see if the test returns a non-zero exit code, indicating a failed test, or the framework can compare the output of the test against the expected output. The output comparison takes options, such as whether case or white space is ignored, and generates an output diff if the comparison fails. There are several ways you can check the output of a test case:
  * Check the output against a constant value
  * Check the output against the contents of a file
  * Check the output against the output of a reference implementation.

If a test case doesn't finish in the expected time, it will be killed and the test case marked as failed due to a timeout. You do _not_ need to implement your own timeout and code to kill non-terminating tests; we'll do that for you.


## Naming test cases ##

You specify the names of the test cases using the property:

`test.cases.`_kind_` = `_names_

For example,

`test.cases.public = p1,p2,p3`

indicates three public test cases, named p1, p2 and p3.

## Test properties ##

Each test can have the following properties:

| **Name** | **Description** |
|:---------|:----------------|
| `exec`   | the command line to be exec'd, using Runtime.exec |
| `input`  | The input provided to the test case. Can be either a single line of text, surrounded by double quotes (i.e., `"`) , or  the name of a file. |
| `options` | A comma separated list of options for comparing the expected and actual output, including any of `trim`,  `ignoreCase`, `ignoreBlankLines` or `ignoreWhitespaceChange`. |
| `expected` | The output that is expected from the test case. Can be either a single line of text, surrounded by double quotes (i.e., `"`) , or  the name of a file |
| `referenceExec` | A command line to be executed that will provide the expected output for the test case. The command line will be given the same input as the student code under test. |


> It is an error to specify both `expected` and `referenceExec` for a test case. You can specify either, or neither. If neither is specified,  the test case passes if it exits with a zero error code.

## Test property defaults ##

You can specify a test property using any of the following:

`test.`_property_ ` = ` _value_

`test.cases.`_kind_`.`_property_ ` = ` _value_

`test.case.`_name`.`_property_` = `_value

The first of these defines a default property. The second describes a default for all test cases of a particular kind (e.g., `public`). The last defines the property for a particular test case.

When looking for a property for a test case, the system first checks for a property defined for that specific test case. If none is found, it next looks for a properly defined for the test case kind.

For all of these properties, the character `&` has a special meaning: it is replaced with the name of the test case.

For example, specifying

`test.input = &.in`

`test.expected = &.expected`

means that test case `p1` will use the file `p1.in` for its input, and the output will be checked against `p1.expected`.

If no property is found for the `exec` property for a test case, the name of the test case is used as the executable. For example, if no `exec` property is found for the test case `p1`, the file `p1` is executed.

### Example, contrived test properties ###

The following properties:
```
test.framework = script

test.cases.public = p1, p2
test.cases.release = r1, r2

test.exec = python &.py
test.cases.release.exec = python2.7 &.py

test.input = &.in
test.cases.public.expected = &.out
test.cases.release.referenceExec = python2.7 &-instructor.py
test.case.r2.input = "input for r2"
```

Define the following test cases:
| **name** | **kind** | **exec** | **input** | **checked against** |
|:---------|:---------|:---------|:----------|:--------------------|
| `p1`     | public   | `python p1.py` | contents of p1.in | contents of p1.out  |
| `p2`     | public   | `python p2.py` | contents of p2.in | contents of p2.out  |
| `r1`     | release  | `python2.7 r1.py` | contents of [r1](https://code.google.com/p/marmoset/source/detail?r=1).in | output of  `python2.7 r1-instructor.py` |
| `r2`     | release  | `python2.7 r2.py` | `input for r2` | output of  `python2.7 r2-instructor.py` |