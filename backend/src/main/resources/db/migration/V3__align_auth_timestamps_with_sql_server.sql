DECLARE @constraintName nvarchar(200);
DECLARE @sql nvarchar(max);

SELECT @constraintName = dc.name
FROM sys.default_constraints dc
JOIN sys.columns c ON c.default_object_id = dc.object_id
JOIN sys.tables t ON t.object_id = c.object_id
WHERE t.name = 'users' AND c.name = 'created_at';
IF @constraintName IS NOT NULL
BEGIN
    SET @sql = N'ALTER TABLE users DROP CONSTRAINT ' + QUOTENAME(@constraintName);
    EXEC sp_executesql @sql;
END

SELECT @constraintName = NULL;
SELECT @constraintName = dc.name
FROM sys.default_constraints dc
JOIN sys.columns c ON c.default_object_id = dc.object_id
JOIN sys.tables t ON t.object_id = c.object_id
WHERE t.name = 'users' AND c.name = 'updated_at';
IF @constraintName IS NOT NULL
BEGIN
    SET @sql = N'ALTER TABLE users DROP CONSTRAINT ' + QUOTENAME(@constraintName);
    EXEC sp_executesql @sql;
END

SELECT @constraintName = NULL;
SELECT @constraintName = dc.name
FROM sys.default_constraints dc
JOIN sys.columns c ON c.default_object_id = dc.object_id
JOIN sys.tables t ON t.object_id = c.object_id
WHERE t.name = 'refresh_tokens' AND c.name = 'expires_at';
IF @constraintName IS NOT NULL
BEGIN
    SET @sql = N'ALTER TABLE refresh_tokens DROP CONSTRAINT ' + QUOTENAME(@constraintName);
    EXEC sp_executesql @sql;
END

SELECT @constraintName = NULL;
SELECT @constraintName = dc.name
FROM sys.default_constraints dc
JOIN sys.columns c ON c.default_object_id = dc.object_id
JOIN sys.tables t ON t.object_id = c.object_id
WHERE t.name = 'refresh_tokens' AND c.name = 'revoked_at';
IF @constraintName IS NOT NULL
BEGIN
    SET @sql = N'ALTER TABLE refresh_tokens DROP CONSTRAINT ' + QUOTENAME(@constraintName);
    EXEC sp_executesql @sql;
END

SELECT @constraintName = NULL;
SELECT @constraintName = dc.name
FROM sys.default_constraints dc
JOIN sys.columns c ON c.default_object_id = dc.object_id
JOIN sys.tables t ON t.object_id = c.object_id
WHERE t.name = 'refresh_tokens' AND c.name = 'created_at';
IF @constraintName IS NOT NULL
BEGIN
    SET @sql = N'ALTER TABLE refresh_tokens DROP CONSTRAINT ' + QUOTENAME(@constraintName);
    EXEC sp_executesql @sql;
END

ALTER TABLE users ALTER COLUMN created_at datetimeoffset(7) NOT NULL;
ALTER TABLE users ALTER COLUMN updated_at datetimeoffset(7) NOT NULL;
ALTER TABLE refresh_tokens ALTER COLUMN expires_at datetimeoffset(7) NOT NULL;
ALTER TABLE refresh_tokens ALTER COLUMN revoked_at datetimeoffset(7) NULL;
ALTER TABLE refresh_tokens ALTER COLUMN created_at datetimeoffset(7) NOT NULL;

ALTER TABLE users ADD CONSTRAINT df_users_created_at DEFAULT sysdatetimeoffset() FOR created_at;
ALTER TABLE users ADD CONSTRAINT df_users_updated_at DEFAULT sysdatetimeoffset() FOR updated_at;
ALTER TABLE refresh_tokens ADD CONSTRAINT df_refresh_tokens_created_at DEFAULT sysdatetimeoffset() FOR created_at;