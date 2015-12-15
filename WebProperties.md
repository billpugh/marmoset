# Property files #
These properties can be set in one of two places: `local.web.properties` and `web.properties`. The local version should contain sensitive information like database username and passwords and instance-specific configuration. The values in `local.web.properties` will override those in `web.properties.`

All properties are required.

| **Property Key** | **Description**|
|:-----------------|:|
| `database.user`  | Username for the MySQL database. |
| `database.password` | Password for the MySQL database. |
| `database.driver` | The driver used to access the MySQL database. |
| `database.server.jdbc.url` | The JDBC url for the database. |
| `database.options` | Additional options for accessing the database; they are appended to the JDBC url. |
| `semester`       | Short identifier for the semester. |
| `semesterName`   | Human-readable name for the semester. |
| `admin.email`    | Email address of the admin user. |
| `admin.smtp`     | SMTP server for sending email to the admin. |

TODO(rwsims): Uses of semester vs. semesterName?