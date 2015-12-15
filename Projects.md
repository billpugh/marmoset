# Create a Project #
To define a new project, click the "create new project" link in the instructor
course view. Once created, the project will be invisible to students, but
instructors can view, modify and submit to it as normal. Once the project is
ready to be assigned, click "Make Visible" in the project utilities view to
allow students to view the project.

# Instructor Submissions #
The project needs 3 submissions from an instructor:
  1. A baseline submission
  1. A canonical solution
  1. A test setup
Additionally, instructors can submit normally and have their code tested against the active test setup.

## Baseline ##
A baseline submission should represent the code distributed to the students, if any; it is optional but **highly** recommended. When viewing or [reviewing](CodeReview.md) a student submission, the submit server will diff the submission against the baseline and elide unchanged lines from the view. It is also possible to use the baseline submission as "starter files" for the project.

## Canonical ##
The canonical submission is the "correct" implementation of the project. It's tested against the test setup to confirm that the test setup is correct.

## Test Setup ##
The test setup includes test cases and the `test.properties` metadata file. The specifics of a test setup depend on the language being used, but all languages must include the `test.properties` file in their test setup submission. See the page on [test setup properties](TestSetupProperties.md) for more details.

# Assigning Points #
When the instructor has uploaded a canonical submission and a test setup, they will be built and tested together by any running build servers. Once the test setup is working as intended, click the "assign points" link next to the tested submission in the table on the instructor utilities view for the course. There the instructor can assign point values for each detected test.