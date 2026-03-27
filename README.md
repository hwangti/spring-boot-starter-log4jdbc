Log4jdbc Spring Boot Starter
============================

The Log4jdbc Spring Boot Starter facilitates the quick and easy use of log4jdbc in Spring Boot projects.

Log4jdbc is particularly handy as it can log SQL that is ready to run.
Instead of logging SQL with '?' where parameter values need to be inserted (like, for example,
what `spring.jpa.properties.hibernate.show_sql=true` does), log4jdbc can log SQL with those placeholders substituted
with their actual values. So instead of `select name from User where id = ?` the log will say `select name from User where id = 5`.

Forked from [this repo](https://github.com/candrews/log4jdbc-spring-boot-starter) as it seems to be no longer maintained.
Received assistance with configuration values from [this repo](https://github.com/andrey-vasilyev/spring-boot-starter-log4jdbc).

Quick Start
===========
**Minimum requirements** — You'll need Java 8+ and Spring Boot 2.7+.
1. Make sure Maven Central is on the repository list and add a dependency (see below)
2. Enable a logger (for example, add `logging.level.jdbc.sqlonly=INFO` to `application.properties`), check the list of [Loggers](#loggers) for details.

#### Gradle
```groovy
    implementation("kr.hwangti:spring-boot-starter-log4jdbc:${version}")
```

#### Maven
```xml
  <dependency>
    <groupId>kr.hwangti</groupId>
    <artifactId>spring-boot-starter-log4jdbc</artifactId>
    <version>${version}</version>
  </dependency>
```

Configuration
=============
Use `application.properties` to configure log4jdbc. There is [Spring configuration metadata](http://docs.spring.io/spring-boot/docs/current/reference/html/configuration-metadata.html) for IDE autocompletion. Supported settings are:

| property                                   | default                                        | description                                                                                                                                                                                                                                       |
|--------------------------------------------|------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| log4jdbc.auto.load.popular.drivers         | true                                           | Set this to false to disable the feature where popular drivers are automatically loaded. If this is false, you must set the `log4jdbc.drivers` property in order to load the driver(s) you want.                                                  |
| log4jdbc.debug.stack.prefix                |                                                | A REGEX matching the package name of your application. The call stack will be searched down to the first occurrence of a class that has the matching REGEX. Example: `^com\.mycompany\.myapp.*`                                                   |
| log4jdbc.drivers                           |                                                | One or more fully qualified class names for JDBC drivers that log4jdbc should load and wrap (comma separated). This option is not normally needed because most popular JDBC drivers are already loaded by default.                                |
| log4jdbc.dump.booleanastruefalse           | false                                          | When dumping boolean values in SQL, dump them as 'true' or 'false'. If not set, they will be dumped as 1 or 0.                                                                                                                                    |
| log4jdbc.dump.fulldebugstacktrace          | false                                          | If dumping in debug mode, dump the full stack trace. This will result in extremely voluminous output.                                                                                                                                             |
| log4jdbc.dump.sql.addsemicolon             | false                                          | Set this to true to add an extra semicolon to the end of SQL in the output.                                                                                                                                                                       |
| log4jdbc.dump.sql.create                   | true                                           | Set this to false to suppress SQL create statements in the output.                                                                                                                                                                                |
| log4jdbc.dump.sql.delete                   | true                                           | Set this to false to suppress SQL delete statements in the output.                                                                                                                                                                                |
| log4jdbc.dump.sql.insert                   | true                                           | Set this to false to suppress SQL insert statements in the output.                                                                                                                                                                                |
| log4jdbc.dump.sql.maxlinelength            | 0                                              | When dumping SQL, if this is greater than 0, the dumped SQL will be broken up into lines that are no longer than this value. Set to 0 to disable.                                                                                                 |
| log4jdbc.dump.sql.select                   | true                                           | Set this to false to suppress SQL select statements in the output.                                                                                                                                                                                |
| log4jdbc.dump.sql.update                   | true                                           | Set this to false to suppress SQL update statements in the output.                                                                                                                                                                                |
| log4jdbc.format.sql                        | true                                           | Set this to true to enable pretty-printing of SQL using Hibernate's `FormatStyle`. DDL statements use DDL formatting, other statements use BASIC formatting.                                                                                      |
| log4jdbc.highlight.sql                     | true                                           | Set this to true to enable ANSI syntax highlighting of SQL using Hibernate's `FormatStyle.HIGHLIGHT`. Requires a terminal that supports ANSI escape codes. When logging to files, consider stripping ANSI codes via Logback's `%replace` pattern. |
| log4jdbc.log4j2.properties.file            | log4jdbc.log4j2.properties                     | Set the name of the property file to use.                                                                                                                                                                                                         |
| log4jdbc.resultsettable.maxcolumnwidth     | 50                                             | Maximum display width (in characters) for each column in the result set table output. Columns wider than this value will be truncated with `...`. Set to 0 to disable truncation.                                                                 |
| log4jdbc.resultsettable.maxrows            | 50                                             | Maximum number of rows to display in the result set table output. When exceeded, remaining rows are replaced with a `...` ellipsis row and an omitted row count summary. Set to 0 to display all rows.                                            |
| log4jdbc.spy.enabled                       | true                                           | Set this to false to disable log4jdbc. This is useful for production configurations.                                                                                                                                                              |
| log4jdbc.spylogdelegator.name              | net.sf.log4jdbc.log.slf4j.Slf4jSpyLogDelegator | The qualified class name of the SpyLogDelegator to use. The default in this Starter is SLF4J which is different from the log4jdbc library's default.                                                                                              |
| log4jdbc.sqltiming.error.threshold         |                                                | Millisecond time value. Causes SQL that takes the specified time or more to execute to be logged at the error level in the sqltiming log.                                                                                                         |
| log4jdbc.sqltiming.warn.threshold          |                                                | Millisecond time value. Causes SQL that takes the specified time or more to execute to be logged at the warning level in the sqltiming log.                                                                                                       |
| log4jdbc.statement.warn                    | false                                          | Set this to true to display warnings in the log when Statements are used.                                                                                                                                                                         |
| log4jdbc.suppress.generated.keys.exception | false                                          | Set to true to ignore any exception produced by the method `Statement.getGeneratedKeys()`.                                                                                                                                                        |
| log4jdbc.trim.sql                          | true                                           | Set this to false to not trim the logged SQL.                                                                                                                                                                                                     |
| log4jdbc.trim.sql.extrablanklines          | true                                           | Set this to false to not trim extra blank lines in the logged SQL (by default, contiguous blank lines are collapsed to one).                                                                                                                      |

Loggers
=======
Note that, by default, nothing will be logged. In fact, if all the loggers are disabled (which is the default), then log4jdbc doesn't even wrap the `java.sql.Connection` returned by `javax.sql.DataSource.getConnection()` (which is useful for production configurations).

| logger              | description                                                                                                                                                                               |
|---------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| jdbc.sqlonly        | Logs only SQL. SQL executed within a prepared statement is automatically shown with it's bind arguments replaced with the data bound at that position, for greatly increased readability. |
| jdbc.sqltiming      | Logs the SQL, post-execution, including timing statistics on how long the SQL took to execute.                                                                                            |
| jdbc.audit          | Logs ALL JDBC calls except for ResultSets. This is a very voluminous output, and is not normally needed unless tracking down a specific JDBC problem.                                     |
| jdbc.resultset      | Even more voluminous, because all calls to ResultSet objects are logged.                                                                                                                  |
| jdbc.resultsettable | Log the jdbc results as a table. Level debug will fill in unread values in the result set.                                                                                                |
| jdbc.connection     | Logs connection open and close events as well as dumping all open connection numbers. This is very useful for hunting down connection leak problems.                                      |

To set a logger's level, use `application.properties` in the same way any other log level is configured (see the [Spring Boot Logging Guide](https://docs.spring.io/spring-boot/docs/current/reference/html/howto-logging.html) for reference). For example, `logging.level.jdbc.sqlonly=DEBUG`

Result Set Table
================
When `jdbc.resultsettable` is enabled, result sets are displayed as formatted tables with box-drawing characters:

```
5 row(s) fetched
┌─────┬────────────┬────────┬──────────┐
│ id  │ name       │ status │ price    │
├─────┼────────────┼────────┼──────────┤
│   1 │ Widget A   │ ACTIVE │    29900 │
│   2 │ Widget B   │ ACTIVE │    15000 │
│   3 │ Gadget C   │ <NULL> │     8500 │
│ ... │ ...        │ ...    │      ... │
└─────┴────────────┴────────┴──────────┘
 ... 2 row(s) omitted
```

| feature                 | description                                                                                                            |
|-------------------------|------------------------------------------------------------------------------------------------------------------------|
| Box-drawing borders     | Uses Unicode box-drawing characters (`┌─┬┐│├┼┤└┴┘`) for clean table rendering.                                         |
| Numeric right-alignment | Numeric columns (INTEGER, BIGINT, DECIMAL, etc.) are automatically right-aligned.                                      |
| NULL highlighting       | SQL NULL values are displayed as `<NULL>` and rendered in gray when `log4jdbc.highlight.sql=true`.                     |
| Column width limit      | Long column headers/values are truncated with `...` (configurable via `log4jdbc.resultsettable.maxcolumnwidth`).       |
| Row limit               | Large result sets are truncated with an ellipsis row and summary (configurable via `log4jdbc.resultsettable.maxrows`). |
| Row count               | Total fetched row count is displayed before the table.                                                                 |
| CJK support             | CJK characters are calculated with adjusted width for proper column alignment.                                         |
| ANSI highlighting       | Table borders are rendered in gray when `log4jdbc.highlight.sql=true`.                                                 |

### Logging to files

When `log4jdbc.highlight.sql=true`, ANSI escape codes are embedded in log messages. For file appenders, strip them using Logback's `%replace`:

```xml
<appender name="FILE" class="ch.qos.logback.core.FileAppender">
    <encoder>
        <pattern>%replace(%msg){'\x1B\[[0-9;]*m', ''}%n</pattern>
    </encoder>
</appender>
```
