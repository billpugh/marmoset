
# Changes 2/22/2012
# Allow file to be null, and upto 200 characters

ALTER TABLE  `code_review_thread` CHANGE  `file`  `file` VARCHAR( 200 ) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL;

CREATE TABLE `review_requests` (
`submission_pk` INT NOT NULL ,
`course_pk` INT NOT NULL ,
INDEX (  `course_pk` ) ,
UNIQUE (
`submission_pk`
)
) ENGINE = INNODB;


ALTER TABLE  `review_requests` ADD  `when` DATETIME NOT NULL DEFAULT  '2012/02/01',
ADD  `accepted` TINYINT( 1 ) NOT NULL DEFAULT  '0';

ALTER TABLE  `code_review_thread` CHANGE  `file`  `file` VARCHAR( 120 ) CHARACTER SET utf8 COLLATE utf8_general_ci NULL;

ALTER TABLE  `courses` ADD  `help_requests_allowed` TINYINT( 1 ) NOT NULL DEFAULT  '0'

# Changed 3/29/2012

ALTER TABLE  `test_outcomes` CHANGE  `exception_class_name`  `exception_class_name` TEXT CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL

# change 2/25/2013

ALTER TABLE  `code_reviewer` ADD  `last_seen` DATETIME NULL DEFAULT NULL COMMENT  'last time this reviewer examined the code review' AFTER  `last_update`;
ALTER TABLE  `code_reviewer` ADD  `rating` INT NOT NULL DEFAULT  '0' COMMENT  'Rating of reviewer by author (1-5)';
ALTER TABLE  `code_reviewer` ADD  `rating_comment` TEXT CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT  'Comments from author explaining the rating they gave a reviewer';


# change 3/8/2013

ALTER TABLE  `code_review_assignment` ADD  `visible_to_students` TINYINT( 1 ) NOT NULL DEFAULT  '1'

# change 4/30/2013

ALTER TABLE  `courses` CHANGE  `url`  `url` TEXT CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL;
ALTER TABLE  `projects` CHANGE  `URL`  `URL` TEXT CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL;
ALTER TABLE  `projects` CHANGE  `title`  `title` TEXT CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL;

# change 5/1/2013

ALTER TABLE  `test_runs` ADD  `test_duration_millis` INT NOT NULL DEFAULT  '0' COMMENT  'Build and test time, in ms';


# change 8/7/2013 - make checksums 40 characters, to handle SHA-1

ALTER TABLE  `submission_archives` CHANGE  `checksum`  `checksum` VARCHAR( 40 ) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL;
ALTER TABLE  `test_setup_archives` CHANGE  `checksum`  `checksum` VARCHAR( 40 ) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL;
ALTER TABLE  `eclipse_launch_events` CHANGE  `checksum`  `checksum` VARCHAR( 40 ) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '';
ALTER TABLE  `code_metrics` CHANGE  `checksum_sourcefiles`  `checksum_sourcefiles` VARCHAR( 40 ) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '';
ALTER TABLE  `code_metrics` CHANGE  `checksum_classfiles`  `checksum_classfiles` VARCHAR( 40 ) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '';
ALTER TABLE  `test_runs` CHANGE  `checksum_sourcefiles`  `checksum_sourcefiles` VARCHAR( 40 ) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL;
ALTER TABLE  `test_runs` CHANGE  `checksum_classfiles`  `checksum_classfiles` VARCHAR( 40 ) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL;

# change 8/9/2013 - add tables for file contents

CREATE TABLE `archive_contents` (
  `archive_pk` int(11) NOT NULL,
  `file_pk` int(11) NOT NULL,
  KEY `submission_pk` (`archive_pk`,`file_pk`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

CREATE TABLE `file_contents` (
  `file_pk` int(11) NOT NULL AUTO_INCREMENT,
  `name` text CHARACTER SET utf8 NOT NULL,
  `text` tinyint(1) NOT NULL,
  `size` int(11) NOT NULL,
  `checksum` varchar(40) CHARACTER SET utf8 NOT NULL,
  `contents` mediumblob NOT NULL,
  PRIMARY KEY (`file_pk`),
  UNIQUE KEY `checksum` (`checksum`),
  KEY `text` (`text`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 COLLATE=utf8_bin;



ALTER TABLE  `student_submit_status` ADD  `last_build_request_timestamp` DATETIME NULL DEFAULT NULL;

ALTER TABLE  `buildservers` ADD  `java_version` VARCHAR( 40 ) NOT NULL DEFAULT  'UNKNOWN';
