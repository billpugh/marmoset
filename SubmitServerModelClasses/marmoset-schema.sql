-- phpMyAdmin SQL Dump
-- version 3.3.9.2
-- http://www.phpmyadmin.net
--
-- Host: localhost
-- Generation Time: Apr 11, 2013 at 05:05 PM
-- Server version: 5.5.9
-- PHP Version: 5.3.6

SET SQL_MODE="NO_AUTO_VALUE_ON_ZERO";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;

--
-- Database: `submitserver`
--

-- --------------------------------------------------------

-- Table structure for table `archive_contents`
--

CREATE TABLE `archive_contents` (
  `archive_pk` int(11) NOT NULL,
  `file_pk` int(11) NOT NULL,
  KEY `submission_pk` (`archive_pk`,`file_pk`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

-- --------------------------------------------------------

--
-- Table structure for table `buildservers`
--

CREATE TABLE `buildservers` (
  `buildserver_pk` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(100) COLLATE utf8_bin NOT NULL,
  `remote_host` text COLLATE utf8_bin NOT NULL,
  `courses` text COLLATE utf8_bin NOT NULL,
  `java_version` VARCHAR( 40 ) NOT NULL DEFAULT 'UNKNOWN',
  `last_request` datetime NOT NULL,
  `kind` VARCHAR( 40 ) NOT NULL DEFAULT 'UNKNOWN',
  `last_request_submission_pk` int(11) NOT NULL DEFAULT '0',
  `last_job` datetime DEFAULT NULL,
  `last_success` datetime DEFAULT NULL,
  `system_load` text COLLATE utf8_bin NOT NULL,
  `last_built_submission_pk` int(11) NOT NULL DEFAULT '0',
  `last_testrun_pk` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`buildserver_pk`),
  UNIQUE KEY `name` (`name`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

-- --------------------------------------------------------

--
-- Table structure for table `code_metrics`
--

CREATE TABLE `code_metrics` (
  `test_run_pk` mediumint(6) unsigned NOT NULL DEFAULT '0',
  `checksum_sourcefiles` varchar(40) NOT NULL DEFAULT '',
  `checksum_classfiles` varchar(40) NOT NULL DEFAULT '',
  `code_segment_size` mediumint(8) NOT NULL DEFAULT '0',
  PRIMARY KEY (`test_run_pk`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Table structure for table `code_reviewer`
--

CREATE TABLE `code_reviewer` (
  `code_reviewer_pk` int(11) NOT NULL AUTO_INCREMENT,
  `code_review_assignment_pk` int(11) NOT NULL,
  `submission_pk` int(11) NOT NULL,
  `student_pk` int(11) NOT NULL,
  `is_author` tinyint(1) NOT NULL,
  `is_instructor` tinyint(1) NOT NULL DEFAULT '0',
  `last_update` datetime DEFAULT NULL,
  `last_seen` datetime DEFAULT NULL COMMENT 'last time this reviewer examined the code review',
  `num_comments` int(11) NOT NULL DEFAULT '0',
  `known_as` varchar(100) NOT NULL,
  `is_automated` tinyint(1) NOT NULL DEFAULT '0',
  `rating` int(11) NOT NULL DEFAULT '0' COMMENT 'Rating of reviewer by author (1-5)',
  `rating_comment` text COMMENT 'Comments from author explaining the rating they gave a reviewer',
  PRIMARY KEY (`code_reviewer_pk`),
  KEY `code_review_pk` (`student_pk`),
  KEY `code_review_assignment_pk` (`code_review_assignment_pk`),
  KEY `submission_pk` (`submission_pk`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 COMMENT='Assignment of a reviewer to a code review';

-- --------------------------------------------------------

--
-- Table structure for table `code_review_assignment`
--

CREATE TABLE `code_review_assignment` (
  `code_review_assignment_pk` int(11) NOT NULL AUTO_INCREMENT,
  `description` text NOT NULL,
  `project_pk` int(11) NOT NULL,
  `deadline` datetime NOT NULL,
  `other_reviews_visible` tinyint(1) NOT NULL,
  `anonymous` tinyint(1) NOT NULL DEFAULT '0',
  `kind` varchar(30) NOT NULL DEFAULT 'INSTRUCTIONAL',
  `visible_to_students` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`code_review_assignment_pk`),
  KEY `project_pk` (`project_pk`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Table structure for table `code_review_comment`
--

CREATE TABLE `code_review_comment` (
  `code_review_comment_pk` int(11) NOT NULL AUTO_INCREMENT,
  `code_reviewer_pk` int(11) NOT NULL,
  `comment` mediumtext NOT NULL,
  `modified` datetime NOT NULL,
  `draft` int(11) NOT NULL,
  `ack` tinyint(1) NOT NULL DEFAULT '1',
  `code_review_thread_pk` int(11) NOT NULL,
  PRIMARY KEY (`code_review_comment_pk`),
  KEY `code_reviewer_pk` (`code_reviewer_pk`),
  KEY `code_review_thread_pk` (`code_review_thread_pk`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Table structure for table `code_review_thread`
--

CREATE TABLE `code_review_thread` (
  `code_review_thread_pk` int(11) NOT NULL AUTO_INCREMENT,
  `file` varchar(120) DEFAULT NULL,
  `line` int(11) NOT NULL DEFAULT '0',
  `created` datetime NOT NULL,
  `created_by` int(11) NOT NULL,
  `submission_pk` int(11) NOT NULL,
  `rubric_evaluation_pk` int(11) NOT NULL,
  PRIMARY KEY (`code_review_thread_pk`),
  KEY `submission_pk` (`submission_pk`),
  KEY `rubric_evaluation_pk` (`rubric_evaluation_pk`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Table structure for table `courses`
--

CREATE TABLE `courses` (
  `course_pk` int(20) unsigned NOT NULL AUTO_INCREMENT,
  `semester` varchar(15) NOT NULL DEFAULT '',
  `coursename` varchar(20) DEFAULT NULL,
  `section` varchar(2) DEFAULT NULL,
  `description` text,
  `url` text DEFAULT NULL,
  `course_ids` varchar(50) DEFAULT NULL,
  `allows_baseline_download` tinyint(1) NOT NULL DEFAULT '1',
  `submit_key` varchar(40) NOT NULL,
  `buildserver_key` varchar(40) NOT NULL,
  `browser_editing` varchar(20) NOT NULL DEFAULT 'prohibited',
  `sections` text,
  `help_requests_allowed` tinyint(1) NOT NULL DEFAULT '0',
  `use_default_buildservers` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`course_pk`),
  UNIQUE KEY `submit_key` (`submit_key`),
  UNIQUE KEY `buildserver_key` (`buildserver_key`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Table structure for table `eclipse_launch_events`
--

CREATE TABLE `eclipse_launch_events` (
  `eclipse_launch_event_pk` mediumint(10) unsigned NOT NULL AUTO_INCREMENT,
  `student_registration_pk` mediumint(10) unsigned NOT NULL DEFAULT '0',
  `project_number` varchar(20) NOT NULL DEFAULT '',
  `project_pk` mediumint(8) unsigned NOT NULL DEFAULT '0',
  `checksum` varchar(40) NOT NULL DEFAULT '',
  `event` varchar(20) NOT NULL DEFAULT '',
  `timestamp` datetime NOT NULL DEFAULT '0000-00-00 00:00:00',
  `skew` mediumint(9) NOT NULL DEFAULT '0',
  PRIMARY KEY (`eclipse_launch_event_pk`),
  KEY `project_pk` (`project_pk`),
  KEY `student_registration_pk` (`student_registration_pk`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Table structure for table `errors`
--

CREATE TABLE `errors` (
  `error_pk` int(11) NOT NULL AUTO_INCREMENT,
  `when` datetime NOT NULL,
  `user_pk` int(11) DEFAULT NULL,
  `student_pk` int(11) DEFAULT NULL,
  `course_pk` int(11) DEFAULT NULL,
  `project_pk` int(11) DEFAULT NULL,
  `submission_pk` int(11) DEFAULT NULL,
  `code` varchar(200) COLLATE utf8_bin DEFAULT NULL,
  `message` text COLLATE utf8_bin,
  `type` varchar(200) COLLATE utf8_bin DEFAULT NULL,
  `servlet` varchar(200) COLLATE utf8_bin DEFAULT NULL,
  `uri` text COLLATE utf8_bin,
  `query_string` text COLLATE utf8_bin,
  `remote_host` varchar(200) COLLATE utf8_bin DEFAULT NULL,
  `referer` varchar(200) COLLATE utf8_bin DEFAULT NULL,
  `throwable_as_string` text COLLATE utf8_bin,
  `throwable` blob,
  `kind` varchar(30) COLLATE utf8_bin DEFAULT NULL,
  `user_agent` text COLLATE utf8_bin,
  PRIMARY KEY (`error_pk`),
  KEY `when` (`when`),
  KEY `user_pk` (`user_pk`),
  KEY `student_pk` (`student_pk`),
  KEY `course_pk` (`course_pk`),
  KEY `project_pk` (`project_pk`),
  KEY `submission_pk` (`submission_pk`),
  KEY `kind` (`kind`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

-- --------------------------------------------------------

--
-- Table structure for table `file_contents`
--

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

-- --------------------------------------------------------

--
-- Table structure for table `help_requests`
--

CREATE TABLE `help_requests` (
  `review_request_pk` int(11) NOT NULL AUTO_INCREMENT,
  `submission_pk` int(11) NOT NULL,
  `course_pk` int(11) NOT NULL,
  `when` datetime NOT NULL DEFAULT '2012-02-01 00:00:00',
  `accepted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`review_request_pk`),
  UNIQUE KEY `submission_pk` (`submission_pk`),
  KEY `course_pk` (`course_pk`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

-- --------------------------------------------------------

--
-- Table structure for table `log_entries`
--

CREATE TABLE `log_entries` (
  `log_pk` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `course_pk` int(10) unsigned NOT NULL,
  `student_pk` int(10) unsigned DEFAULT NULL,
  `priority` int(11) NOT NULL,
  `title` text NOT NULL,
  `published` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `summary` text NOT NULL,
  `link` text NOT NULL,
  PRIMARY KEY (`log_pk`),
  KEY `course_pk` (`course_pk`),
  KEY `student_pk` (`student_pk`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Table structure for table `projects`
--

CREATE TABLE `projects` (
  `project_pk` int(20) unsigned NOT NULL AUTO_INCREMENT,
  `hidden` tinyint(1) NOT NULL DEFAULT '0',
  `course_pk` int(20) unsigned NOT NULL DEFAULT '0',
  `test_setup_pk` int(20) unsigned NOT NULL DEFAULT '0',
  `project_number` varchar(30) NOT NULL DEFAULT '0',
  `ontime` datetime NOT NULL DEFAULT '0000-00-00 00:00:00',
  `late` datetime NOT NULL DEFAULT '0000-00-00 00:00:00',
  `title` text DEFAULT NULL,
  `URL` text DEFAULT NULL,
  `description` text,
  `release_tokens` smallint(3) NOT NULL DEFAULT '0',
  `regeneration_time` int(3) NOT NULL DEFAULT '0',
  `is_tested` tinyint(1) NOT NULL DEFAULT '1',
  `is_pair` tinyint(1) NOT NULL DEFAULT '0',
  `visible_to_students` tinyint(1) NOT NULL,
  `post_deadline_outcome_visibility` enum('nothing','everything') NOT NULL DEFAULT 'nothing',
  `kind_of_late_penalty` enum('constant','multiplier') DEFAULT 'constant',
  `late_multiplier` decimal(3,2) DEFAULT NULL,
  `late_constant` smallint(4) unsigned DEFAULT NULL,
  `canonical_student_registration_pk` int(20) unsigned NOT NULL DEFAULT '0',
  `best_submission_policy` varchar(100) DEFAULT NULL,
  `release_policy` enum('after_public','anytime') DEFAULT 'after_public',
  `stack_trace_policy` enum('test_name_only','restricted_exception_location','exception_location','full_stack_trace') DEFAULT 'test_name_only',
  `num_release_tests_revealed` smallint(6) NOT NULL DEFAULT '2',
  `archive_pk` mediumint(8) unsigned DEFAULT NULL,
  `diff_against` int(11) NOT NULL DEFAULT '0',
  `browser_editing` varchar(20) NOT NULL DEFAULT 'prohibited',
  PRIMARY KEY (`project_pk`),
  KEY `course_pk` (`course_pk`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Table structure for table `registration_requests`
--

CREATE TABLE `registration_requests` (
  `student_pk` int(11) NOT NULL,
  `course_pk` int(11) NOT NULL,
  `timestamp` bigint(20) NOT NULL,
  `status` enum('PENDING','APPROVED','DENIED') COLLATE utf8_bin NOT NULL,
  `section` text COLLATE utf8_bin,
  UNIQUE KEY `student_pk_2` (`student_pk`,`course_pk`),
  KEY `student_pk` (`student_pk`),
  KEY `course_pk` (`course_pk`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

-- --------------------------------------------------------

--
-- Table structure for table `rubrics`
--

CREATE TABLE `rubrics` (
  `rubric_pk` int(11) NOT NULL AUTO_INCREMENT,
  `code_review_assignment_pk` int(11) NOT NULL,
  `name` varchar(80) COLLATE utf8_bin NOT NULL,
  `description` text COLLATE utf8_bin NOT NULL,
  `presentation` varchar(40) COLLATE utf8_bin NOT NULL,
  `data` varchar(120) COLLATE utf8_bin DEFAULT NULL,
  PRIMARY KEY (`rubric_pk`),
  KEY `code_review_assignment_pk` (`code_review_assignment_pk`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

-- --------------------------------------------------------

--
-- Table structure for table `rubric_evaluations`
--

CREATE TABLE `rubric_evaluations` (
  `rubric_evaluation_pk` int(11) NOT NULL AUTO_INCREMENT,
  `rubric_pk` int(11) NOT NULL,
  `code_reviewer_pk` int(11) NOT NULL,
  `code_review_thread_pk` int(11) NOT NULL,
  `explanation` text COLLATE utf8_bin NOT NULL,
  `points` int(11) NOT NULL,
  `value` text COLLATE utf8_bin,
  `status` varchar(10) COLLATE utf8_bin NOT NULL,
  `modified` datetime NOT NULL,
  PRIMARY KEY (`rubric_evaluation_pk`),
  KEY `rubric_pk` (`rubric_pk`,`code_reviewer_pk`,`code_review_thread_pk`),
  KEY `status` (`status`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

-- --------------------------------------------------------

--
-- Table structure for table `students`
--

CREATE TABLE `students` (
  `student_pk` int(20) unsigned NOT NULL AUTO_INCREMENT,
  `login_name` varchar(50) NOT NULL DEFAULT '',
  `campus_uid` varchar(80) NOT NULL DEFAULT '',
  `firstname` varchar(50) DEFAULT NULL,
  `lastname` varchar(50) DEFAULT NULL,
  `email` varchar(100) DEFAULT NULL,
  `superuser` tinyint(1) NOT NULL DEFAULT '0',
  `given_consent` enum('yes','no','under 18','pending') NOT NULL DEFAULT 'pending',
  `account_type` enum('normal','demo','team','admin','pseudo','foobar') NOT NULL DEFAULT 'normal',
  `has_picture` tinyint(1) NOT NULL DEFAULT '0',
  `can_import_courses` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`student_pk`),
  UNIQUE KEY `login_name` (`login_name`),
  KEY `campus_uid` (`campus_uid`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Table structure for table `student_pictures`
--

CREATE TABLE `student_pictures` (
  `student_pk` int(11) NOT NULL,
  `type` varchar(50) NOT NULL,
  `image` blob NOT NULL,
  UNIQUE KEY `student_pk` (`student_pk`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Table structure for table `student_registration`
--

CREATE TABLE `student_registration` (
  `student_registration_pk` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `course_pk` int(5) unsigned NOT NULL DEFAULT '0',
  `student_pk` int(5) unsigned NOT NULL DEFAULT '0',
  `class_account` varchar(100) DEFAULT NULL,
  `instructor_capability` enum('read-only','modify','pseudo-student') DEFAULT NULL,
  `firstname` varchar(50) DEFAULT NULL,
  `lastname` varchar(50) DEFAULT NULL,
  `dropped` tinyint(1) NOT NULL DEFAULT '0',
  `inactive` tinyint(1) NOT NULL DEFAULT '0',
  `course` varchar(50) DEFAULT NULL,
  `section` varchar(50) DEFAULT NULL,
  `courseID` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`student_registration_pk`),
  KEY `student_pk` (`student_pk`),
  KEY `course_pk` (`course_pk`),
  KEY `cvs_account` (`class_account`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Table structure for table `student_submit_status`
--

CREATE TABLE `student_submit_status` (
  `project_pk` int(20) unsigned NOT NULL DEFAULT '0',
  `student_registration_pk` int(20) unsigned NOT NULL DEFAULT '0',
  `partner_sr_pk` int(11) DEFAULT NULL,
  `one_time_password` varchar(20) NOT NULL DEFAULT '',
  `number_submissions` int(20) unsigned NOT NULL DEFAULT '0',
  `number_commits` smallint(10) unsigned NOT NULL DEFAULT '0',
  `number_runs` int(11) NOT NULL DEFAULT '0',
  `extension` smallint(3) unsigned NOT NULL DEFAULT '0',
  `can_release_test` tinyint(1) NOT NULL DEFAULT '1',
  `last_build_request_timestamp` DATETIME NULL DEFAULT NULL,
  PRIMARY KEY (`project_pk`,`student_registration_pk`),
  KEY `partner_sr_pk` (`partner_sr_pk`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Table structure for table `submissions`
--

CREATE TABLE `submissions` (
  `submission_pk` int(20) unsigned NOT NULL AUTO_INCREMENT,
  `student_registration_pk` int(20) unsigned NOT NULL DEFAULT '0',
  `project_pk` int(20) unsigned NOT NULL DEFAULT '0',
  `num_test_outcomes` smallint(4) unsigned NOT NULL DEFAULT '0',
  `current_test_run_pk` int(20) unsigned DEFAULT '0',
  `submission_number` int(20) unsigned NOT NULL DEFAULT '0',
  `submission_timestamp` datetime NOT NULL,
  `most_recent` tinyint(1) NOT NULL DEFAULT '1',
  `cvstag_timestamp` varchar(15) DEFAULT NULL,
  `build_request_timestamp` datetime DEFAULT NULL,
  `build_status` enum('new','pending','complete','accepted','retest','broken') NOT NULL DEFAULT 'new',
  `num_pending_build_requests` int(20) unsigned NOT NULL DEFAULT '0',
  `num_successful_background_retests` int(20) unsigned NOT NULL DEFAULT '0',
  `num_failed_background_retests` int(20) unsigned NOT NULL DEFAULT '0',
  `submit_client` varchar(30) NOT NULL DEFAULT 'unknown',
  `release_request` datetime DEFAULT NULL,
  `release_eligible` tinyint(1) NOT NULL DEFAULT '0',
  `num_passed_overall` smallint(3) NOT NULL DEFAULT '0',
  `num_build_tests_passed` smallint(3) NOT NULL DEFAULT '0',
  `num_public_tests_passed` smallint(3) NOT NULL DEFAULT '0',
  `num_release_tests_passed` smallint(3) NOT NULL DEFAULT '0',
  `num_secret_tests_passed` smallint(3) NOT NULL DEFAULT '0',
  `num_findbugs_warnings` smallint(3) NOT NULL DEFAULT '0',
  `num_changed_lines` int(11) NOT NULL DEFAULT '-1',
  `archive_pk` int(20) unsigned DEFAULT NULL,
  PRIMARY KEY (`submission_pk`),
  KEY `project_pk` (`project_pk`),
  KEY `student_registration_pk` (`student_registration_pk`),
  KEY `build_status` (`build_status`),
  KEY `submission_timestamp` (`submission_timestamp`),
  KEY `current_test_run_pk` (`current_test_run_pk`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Table structure for table `submission_archives`
--

CREATE TABLE `submission_archives` (
  `archive_pk` int(20) unsigned NOT NULL AUTO_INCREMENT,
  `archive` mediumblob NOT NULL,
  `checksum` varchar(40) NOT NULL,
  PRIMARY KEY (`archive_pk`),
  UNIQUE KEY `checksum` (`checksum`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Table structure for table `test_outcomes`
--

CREATE TABLE `test_outcomes` (
  `test_run_pk` int(20) unsigned NOT NULL DEFAULT '0',
  `test_type` enum('build','public','release','secret','findbugs','pmd','student','uncovered_method') NOT NULL DEFAULT 'build',
  `test_number` int(20) unsigned NOT NULL DEFAULT '0',
  `outcome` enum('passed','failed','could_not_run','warning','error','not_implemented','huh','timeout','uncovered_method','missing_component', 'fork_bomb') NOT NULL DEFAULT 'passed',
  `point_value` smallint(4) NOT NULL DEFAULT '0',
  `test_name` varchar(100) NOT NULL DEFAULT '',
  `short_test_result` varchar(300) NOT NULL DEFAULT '',
  `long_test_result` text NOT NULL,
  `exception_class_name` text,
  `coarsest_coverage_level` enum('method','statement','branch','none') DEFAULT NULL,
  `exception_source_covered_elsewhere` tinyint(1) NOT NULL DEFAULT '0',
  `details` mediumblob,
  `execution_time_ms` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`test_run_pk`,`test_type`,`test_number`,`outcome`),
  KEY `test_type` (`test_type`),
  KEY `test_run_pk` (`test_run_pk`),
  KEY `outcome` (`outcome`),
  KEY `test_name` (`test_name`(8))
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Table structure for table `test_runs`
--

CREATE TABLE `test_runs` (
  `test_run_pk` int(20) unsigned NOT NULL AUTO_INCREMENT,
  `test_setup_pk` int(20) unsigned NOT NULL DEFAULT '0',
  `submission_pk` int(20) unsigned NOT NULL DEFAULT '0',
  `test_timestamp` datetime NOT NULL DEFAULT '0000-00-00 00:00:00',
  `test_machine` varchar(100) NOT NULL DEFAULT 'unknown',
  `num_passed_overall` smallint(3) NOT NULL DEFAULT '0',
  `num_build_tests_passed` smallint(3) NOT NULL DEFAULT '0',
  `num_public_tests_passed` smallint(3) NOT NULL DEFAULT '0',
  `num_release_tests_passed` smallint(3) NOT NULL DEFAULT '0',
  `num_secret_tests_passed` smallint(3) NOT NULL DEFAULT '0',
  `num_findbugs_warnings` smallint(3) NOT NULL DEFAULT '0',
  `checksum_classfiles` varchar(40) DEFAULT NULL,
  `checksum_sourcefiles` varchar(40) DEFAULT NULL,
  `test_duration_millis` INT NOT NULL DEFAULT  '0' COMMENT  'Build and test time, in ms',
  PRIMARY KEY (`test_run_pk`),
  KEY `test_runs_ibfk_1` (`submission_pk`),
  KEY `checksum_classfiles` (`checksum_classfiles`(4))
) ENGINE=InnoDB  DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Table structure for table `test_setups`
--

CREATE TABLE `test_setups` (
  `test_setup_pk` int(20) unsigned NOT NULL AUTO_INCREMENT,
  `project_pk` int(20) unsigned NOT NULL DEFAULT '0',
  `jarfile_status` enum('new','pending','tested','active','inactive','failed','broken') NOT NULL DEFAULT 'new',
  `version` smallint(5) unsigned NOT NULL DEFAULT '0',
  `date_posted` datetime DEFAULT NULL,
  `comment` mediumtext,
  `test_run_pk` int(20) unsigned DEFAULT '0',
  `num_total_tests` smallint(3) NOT NULL DEFAULT '0',
  `num_build_tests` smallint(3) NOT NULL DEFAULT '0',
  `num_public_tests` smallint(3) NOT NULL DEFAULT '0',
  `num_release_tests` smallint(3) NOT NULL DEFAULT '0',
  `num_secret_tests` smallint(3) NOT NULL DEFAULT '0',
  `archive_pk` int(20) unsigned DEFAULT NULL,
  PRIMARY KEY (`test_setup_pk`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Table structure for table `test_setup_archives`
--

CREATE TABLE `test_setup_archives` (
  `archive_pk` int(20) NOT NULL AUTO_INCREMENT,
  `archive` mediumblob NOT NULL,
  `checksum` varchar(40) NOT NULL,
  PRIMARY KEY (`archive_pk`),
  UNIQUE KEY `checksum` (`checksum`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

