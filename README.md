# AssetPulse

AssetPulse is a JavaFX desktop application for managing an IT asset inventory. It stores records in MySQL and supports creating, viewing, searching, updating, and deleting assets.

> **Scope:** This is the Phase 1 desktop application. Database access is synchronous on the JavaFX UI thread, so slow database operations can temporarily pause the interface. See [the project guide](docs/PROJECT_GUIDE.md) for architecture, data rules, operational details, and current limitations.

## Features

- View the complete inventory and the total asset count.
- Search by asset tag, name, category, or assignee.
- Add and edit assets, with validation for required fields and database column limits.
- Delete assets only after confirmation.
- Store data in MySQL so it remains available after the application closes.
- Use parameterized JDBC statements and a separate, least-privilege application account.

## Requirements

- JDK 21
- Apache Maven 3.9 or later
- MySQL Server 8.x (MySQL Workbench is a client, not a database server)
- An internet connection for the first Maven build, unless dependencies are already cached

The project uses JavaFX 21 and MySQL Connector/J. VS Code with the Extension Pack for Java is optional but provides a convenient development environment.

## Setup

### 1. Create the database

Start MySQL Server, connect using an administrator account, then execute [`database/schema.sql`](database/schema.sql). This creates the `assetpulse_db` database and the `assets` table.

To load the optional demonstration records, execute [`database/sample_data.sql`](database/sample_data.sql) after the schema. The sample script can be rerun; when an asset tag already exists, it updates that row's asset name.

### 2. Create an application account

Run the following as a MySQL administrator. Replace the example password with a strong, unique password; do not save a real password in a SQL file or commit it to Git.

```sql
CREATE USER 'assetpulse_app'@'localhost'
IDENTIFIED BY 'ReplaceWithYourOwnStrongPassword';

GRANT SELECT, INSERT, UPDATE, DELETE
ON assetpulse_db.*
TO 'assetpulse_app'@'localhost';
```

The app account needs only the four data operations used by this application. Database and table creation are performed separately by an administrator.

### 3. Configure the connection

In the PowerShell terminal from which you will launch the application, set the credentials:

```powershell
$env:ASSETPULSE_DB_USER = "assetpulse_app"
$env:ASSETPULSE_DB_PASSWORD = "your-real-password"
```

The default JDBC URL is `jdbc:mysql://localhost:3306/assetpulse_db?serverTimezone=UTC`. If MySQL is on a different host, port, or database, set the full URL in the same terminal:

```powershell
$env:ASSETPULSE_DB_URL = "jdbc:mysql://db-host:3306/assetpulse_db?serverTimezone=UTC"
```

Environment variables set this way apply only to that terminal session. Configure them again in a new terminal. Never place production credentials in source code, SQL scripts, or committed configuration files.

### 4. Build and run

From the directory containing `pom.xml`:

```powershell
mvn clean verify
mvn javafx:run
```

The first command compiles and verifies the Maven project. The second launches the JavaFX application. To compile without running verification phases, use `mvn clean package`.

## Using the application

1. On launch, the inventory is loaded from MySQL.
2. Select a row to load its values into the details form.
3. Use **Add asset** to create a record, **Update selected** to save changes to the selected record, or **Delete selected** to remove it after confirmation.
4. Enter a search term and choose **Search** to filter by tag, name, category, or assignee. Choose **Refresh** to reload all records.
5. Choose **Clear form** to deselect the current record and reset the fields.

Required fields are asset tag, asset name, category, and purchase cost. Use `0` when the cost is unknown. Status must be one of `AVAILABLE`, `ASSIGNED`, `MAINTENANCE`, or `RETIRED`; purchase date and assignee are optional. Asset tags must be unique.

## Project layout

```text
database/
  schema.sql          Database and table definition
  sample_data.sql     Optional demonstration records
src/main/java/
  com/assetpulse/
    MainApp.java      JavaFX screen, actions, and form validation
    config/Database.java
                      JDBC connection configuration
    model/Asset.java  Asset data model
    repository/AssetRepository.java
                      Parameterized SQL and row mapping
src/main/resources/
  styles.css          JavaFX styles
docs/
  PROJECT_GUIDE.md    Architecture, data model, operations, and release notes
pom.xml               Maven dependencies and JavaFX launch configuration
```

## Troubleshooting

| Problem | What to check |
| --- | --- |
| `mvn` is not recognized | Install Maven, add its `bin` directory to `PATH`, and open a new terminal. |
| Java version/build error | Confirm `java -version` reports JDK 21 and Maven uses that JDK (`mvn -version`). |
| Cannot reach MySQL | Start MySQL Server and verify host, port, and `ASSETPULSE_DB_URL`. Workbench alone does not run the server. |
| Access denied | Check `ASSETPULSE_DB_USER`, `ASSETPULSE_DB_PASSWORD`, and the account's host grant (`'assetpulse_app'@'localhost'`). |
| Unknown database or table | Run `database/schema.sql` as a database administrator. |
| Duplicate entry | Choose an asset tag not already in use. |
| SQL syntax/collation error | Confirm the server is MySQL 8.x and not a different database product. |

## Before sharing or pushing

- Run `mvn clean verify`.
- Confirm the database scripts contain no real credentials.
- Keep credentials in environment variables or an untracked local secrets manager.
- Review `git status` and stage only the intended project files. Do not publish generated output (`target/`), local environment files, or unrelated files.

For code structure, database behavior, validation, and known limitations, see [docs/PROJECT_GUIDE.md](docs/PROJECT_GUIDE.md).
