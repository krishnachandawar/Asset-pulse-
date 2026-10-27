# AssetPulse Project Guide

This guide documents the implementation and operating model of AssetPulse Phase 1. For a first-time installation and the short run instructions, start with the [README](../README.md).

## Project boundaries

AssetPulse is a single-user desktop inventory client backed by a MySQL database. The JavaFX application and database server are separate processes: the app connects to the configured MySQL server over JDBC for each repository operation. This project does not include user authentication, role management, a web service, automated schema migrations, or an installer.

The application currently performs database operations synchronously on the JavaFX Application Thread. This keeps the Phase 1 implementation straightforward, but the UI can pause while a query or connection is slow. Treat the current release as a small internal desktop application, not as a multi-user, high-availability, or internet-facing asset-management service.

## Architecture

```text
JavaFX controls and actions
        |
        v
MainApp (presentation, validation, user feedback)
        |
        v
AssetRepository (CRUD, search, SQL-to-model mapping)
        |
        v
Database (JDBC connection factory)
        |
        v
MySQL: assetpulse_db.assets
```

### Responsibilities

- **`MainApp`** builds the JavaFX screen, handles form actions and selection, validates user input, invokes the repository, and presents success or error messages.
- **`Asset`** is an immutable data object for one row from the `assets` table.
- **`AssetRepository`** contains the SQL for list, search, insert, update, delete, and count operations. It uses `PreparedStatement`, closes JDBC resources with try-with-resources, and maps SQL dates to `LocalDate`.
- **`Database`** reads connection settings from environment variables and opens MySQL connections. It does not keep a shared connection open.
- **`styles.css`** contains the JavaFX presentation styles.

## Configuration and credentials

| Variable | Required | Default | Purpose |
| --- | --- | --- | --- |
| `ASSETPULSE_DB_USER` | Yes | None | MySQL account used by the app |
| `ASSETPULSE_DB_PASSWORD` | Yes | None | Password for the app account |
| `ASSETPULSE_DB_URL` | No | `jdbc:mysql://localhost:3306/assetpulse_db?serverTimezone=UTC` | JDBC URL for the MySQL server and database |

`Database` reads these values when the class is initialized. Set them in the process environment before launching the application. Do not add credentials to Java source, committed SQL, Maven configuration, or documentation. The application account should not be used for schema administration.

The default URL includes `serverTimezone=UTC`; retain appropriate JDBC URL parameters if changing the host or port. The account grant in the README is scoped to the local MySQL host and database. Adjust the MySQL account host component only when the deployment topology requires it, and restrict it to the narrowest applicable host.

## Database model

`database/schema.sql` creates the `assetpulse_db` database using `utf8mb4` and creates the `assets` table.

| Column | Type | Rules and use |
| --- | --- | --- |
| `asset_id` | `INT` | Auto-increment primary key |
| `asset_tag` | `VARCHAR(30)` | Required, unique inventory identifier |
| `asset_name` | `VARCHAR(100)` | Required display name |
| `category` | `VARCHAR(50)` | Required category |
| `assigned_to` | `VARCHAR(100)` | Optional assignee |
| `status` | `ENUM` | Required; `AVAILABLE`, `ASSIGNED`, `MAINTENANCE`, or `RETIRED` |
| `purchase_cost` | `DECIMAL(12,2)` | Required; defaults to zero and cannot be negative |
| `purchase_date` | `DATE` | Optional purchase date |
| `created_at` | `TIMESTAMP` | Set when the row is created |
| `updated_at` | `TIMESTAMP` | Updated automatically when the row changes |

Indexes are defined for status and category. The primary key is used for record selection and mutation; asset tags are protected by a unique constraint. The schema script creates the database and table if they do not already exist. It is an initial schema setup script, not a versioned migration system.

`database/sample_data.sql` inserts three demonstration assets. It uses `ON DUPLICATE KEY UPDATE` on asset tag conflicts and updates the existing row's `asset_name`; it is intended only for sample/demo data.

## Data operations and behavior

- **List/refresh:** Returns all records ordered by descending asset ID. The displayed total is retrieved with a separate count query.
- **Search:** Matches a substring in asset tag, name, category, or assignee and orders matches by descending asset ID. Search input is bound as a parameter rather than concatenated into SQL.
- **Create:** Inserts all editable values and returns the generated ID.
- **Update:** Updates the selected record by primary key. If the record has been removed elsewhere, the UI reports that it no longer exists.
- **Delete:** Requires explicit confirmation and deletes by primary key.
- **Connections:** Each repository call opens and closes its own JDBC connection and statement/result resources.

## Validation and database constraints

The form checks that tag, name, category, status, and purchase cost are present; checks string lengths against their database columns; accepts only a non-negative decimal cost with at most two fractional digits and a maximum precision of 12; and accepts blank assignee/date values as SQL `NULL`. Database constraints remain the final authority for uniqueness and valid stored values.

All database queries use prepared statements. Validation improves error feedback but is not a substitute for database constraints. If the application gains additional clients or a service/API layer, enforce the same data rules at that shared boundary too.

## Development workflow

### Prerequisites

- JDK 21
- Maven 3.9+
- MySQL Server 8.x for interactive database testing

### Build and launch

Run from the project directory:

```powershell
mvn clean verify
mvn javafx:run
```

Set the database environment variables in the same PowerShell session before running `mvn javafx:run`. Maven downloads dependencies during the first build when they are not already cached.

### Changing the database

1. Update `database/schema.sql` for a new installation.
2. If existing installations need a data-preserving change, provide and document a migration; editing the initial schema alone does not update an existing database.
3. Update `Asset`, the repository mapping/SQL, and the JavaFX form/table as needed.
4. Keep SQL parameters bound with `PreparedStatement`.
5. Verify the app against a disposable MySQL database and review both a fresh schema setup and an upgrade path.

### Automated checks

`mvn clean verify` is the project-level build/verification command. There are currently no test sources in the project, so database behavior and JavaFX interaction still require manual verification against a local MySQL instance. When adding tests, keep unit tests independent of developer credentials and use an isolated test database for integration coverage.

## Release checklist

- Run `mvn clean verify` with JDK 21.
- Start the app using a least-privilege MySQL account and verify initial load, search, add, update, and confirmed delete.
- Verify duplicate tags, invalid costs, optional assignee/date, and a database connection failure produce clear feedback.
- Check schema creation against a clean MySQL 8.x database and ensure the required app account grants are documented.
- Search changed SQL/configuration files for accidental credentials.
- Review `git status` and the staged diff; include only intended source, SQL, build, and documentation files.
- Confirm generated build output, local credentials, and unrelated workstation files are not included.

## Known limitations and next steps

- **UI responsiveness:** JDBC calls run on the JavaFX Application Thread. Move I/O to JavaFX `Task` or an executor before using a slow/remote database or a larger inventory.
- **No authentication/audit trail:** The app relies on database account permissions and does not record who changed an asset.
- **No paging:** The entire matching inventory is loaded into the table.
- **No schema migration framework:** Existing installations need explicit migration scripts for schema changes.
- **No automated tests yet:** Add validation, repository, and UI tests as the behavior and deployment requirements grow.
- **Operational deployment:** The application requires a reachable MySQL server and manually configured credentials; packaging, backups, monitoring, and recovery procedures are deployment responsibilities.
