# Liquibase Database Migration Guidelines

## Overview
This project uses Liquibase for database schema versioning and migration management. Liquibase changesets are written in YAML format and executed automatically at application startup.

## Directory Structure
```
src/main/resources/db/
  changelog/
    db.changelog-master.yaml          # Master changelog file
    changes/
      001-init-schema.yaml            # Initial schema
      002-add-folders-table.yaml      # Example: new feature
      003-add-documents-table.yaml    # Example: new feature
      ...
```

## Master Changelog
The master file (`db.changelog-master.yaml`) includes all changesets in order:

```yaml
databaseChangeLog:
  - include:
      file: db/changelog/changes/001-init-schema.yaml
  - include:
      file: db/changelog/changes/002-add-folders-table.yaml
  - include:
      file: db/changelog/changes/003-add-documents-table.yaml
```

## Changeset Naming Convention
- **Format**: `XXX-description.yaml`
  - `XXX`: sequential number (001, 002, 003, ...)
  - `description`: brief description in kebab-case
- **Examples**:
  - `001-init-schema.yaml`
  - `002-add-folders-table.yaml`
  - `010-add-index-folder-parent.yaml`
  - `015-alter-document-add-version.yaml`

## Changeset Structure
Each changeset file must contain:
- `databaseChangeLog` root element
- One or more `changeSet` entries with unique `id` and `author`

### Example: Create Table
```yaml
databaseChangeLog:
  - changeSet:
      id: create-folders-table
      author: developer-name
      changes:
        - createTable:
            tableName: folders
            columns:
              - column:
                  name: id
                  type: UUID
                  constraints:
                    primaryKey: true
                    nullable: false
              - column:
                  name: parent_id
                  type: UUID
                  constraints:
                    nullable: true
                    foreignKeyName: fk_folder_parent
                    references: folders(id)
              - column:
                  name: name
                  type: VARCHAR(255)
                  constraints:
                    nullable: false
              - column:
                  name: acl_mode
                  type: VARCHAR(20)
                  constraints:
                    nullable: false
              - column:
                  name: created_at
                  type: TIMESTAMP
                  defaultValueComputed: CURRENT_TIMESTAMP
              - column:
                  name: created_by
                  type: VARCHAR(255)
```

### Example: Add Column
```yaml
databaseChangeLog:
  - changeSet:
      id: add-folder-description
      author: developer-name
      changes:
        - addColumn:
            tableName: folders
            columns:
              - column:
                  name: description
                  type: VARCHAR(1000)
                  constraints:
                    nullable: true
```

### Example: Create Index
```yaml
databaseChangeLog:
  - changeSet:
      id: create-index-folder-parent
      author: developer-name
      changes:
        - createIndex:
            indexName: idx_folder_parent_id
            tableName: folders
            columns:
              - column:
                  name: parent_id
```

### Example: Insert Reference Data
```yaml
databaseChangeLog:
  - changeSet:
      id: insert-default-roles
      author: developer-name
      changes:
        - insert:
            tableName: roles
            columns:
              - column:
                  name: id
                  value: "1"
              - column:
                  name: name
                  value: "ADMIN"
        - insert:
            tableName: roles
            columns:
              - column:
                  name: id
                  value: "2"
              - column:
                  name: name
                  value: "COLLABORATOR"
```

## Best Practices

### 1. Never Modify Existing Changesets
Once a changeset has been executed in any environment (dev, test, prod), **never modify it**.
- Create a new changeset to make additional changes
- Liquibase uses checksums to detect modifications and will fail

### 2. Use Descriptive IDs
```yaml
# Good
id: create-documents-table
id: add-index-document-folder

# Bad
id: changeset-1
id: update
```

### 3. Include Rollback When Possible
```yaml
databaseChangeLog:
  - changeSet:
      id: add-folder-description
      author: developer-name
      changes:
        - addColumn:
            tableName: folders
            columns:
              - column:
                  name: description
                  type: VARCHAR(1000)
      rollback:
        - dropColumn:
            tableName: folders
            columnName: description
```

### 4. Use Preconditions for Safety
```yaml
databaseChangeLog:
  - changeSet:
      id: add-document-status
      author: developer-name
      preConditions:
        - onFail: MARK_RAN
        - not:
            - columnExists:
                tableName: documents
                columnName: status
      changes:
        - addColumn:
            tableName: documents
            columns:
              - column:
                  name: status
                  type: VARCHAR(50)
```

### 5. Group Related Changes
Keep related schema changes in the same changeset when they must be atomic:
```yaml
databaseChangeLog:
  - changeSet:
      id: refactor-permissions-model
      author: developer-name
      changes:
        - createTable:
            tableName: acl_entries
            # ... columns
        - addForeignKeyConstraint:
            baseTableName: acl_entries
            baseColumnNames: folder_id
            referencedTableName: folders
            referencedColumnNames: id
        - createIndex:
            indexName: idx_acl_folder
            tableName: acl_entries
            columns:
              - column:
                  name: folder_id
```

### 6. Use Context for Environment-Specific Changes
```yaml
databaseChangeLog:
  - changeSet:
      id: insert-test-data
      author: developer-name
      context: test
      changes:
        - insert:
            tableName: folders
            columns:
              - column:
                  name: id
                  value: "test-folder-001"
```

### 7. Naming Conventions for Constraints
- Primary keys: `pk_{table_name}`
- Foreign keys: `fk_{table_name}_{referenced_table}`
- Unique constraints: `uk_{table_name}_{column_name}`
- Indexes: `idx_{table_name}_{column_name}`

## Testing Changesets Locally

### 1. Validate Syntax
```bash
mvn liquibase:validate
```

### 2. Check Pending Changes
```bash
mvn liquibase:status
```

### 3. Generate SQL (Dry Run)
```bash
mvn liquibase:updateSQL
```

### 4. Apply Changes
Application startup automatically applies pending changesets.

## Troubleshooting

### Checksum Validation Failed
If Liquibase detects a modified changeset:
```
Validation Failed: changeset id=xxx checksum changed
```

**Solution**: Never modify executed changesets. Create a new changeset instead.

### Manual Checksum Reset (Development Only)
```sql
-- Only in local development!
UPDATE databasechangelog 
SET md5sum = NULL 
WHERE id = 'changeset-id';
```

### Skip a Failed Changeset (Emergency Only)
```sql
-- Mark as executed without running
INSERT INTO databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum) 
VALUES ('changeset-id', 'author', 'path/to/file.yaml', NOW(), 999, 'MARK_RAN', NULL);
```

## Integration with Docker Compose
The database container in `docker-compose.yml` provides a clean PostgreSQL instance. Liquibase runs automatically when the application starts, creating the schema from scratch.

## Production Considerations
- Always test changesets in dev/test environments first
- Review generated SQL before production deployment
- Consider maintenance windows for large schema changes
- Keep rollback scripts ready for critical changes
- Monitor Liquibase execution logs during deployment

## References
- Official Documentation: https://docs.liquibase.com/
- YAML Format: https://docs.liquibase.com/concepts/changelogs/yaml-format.html
- Changeset Examples: https://docs.liquibase.com/change-types/home.html

