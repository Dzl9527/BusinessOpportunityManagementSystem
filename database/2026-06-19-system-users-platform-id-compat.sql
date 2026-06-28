-- Align legacy WeCom user identity with the current platform user identity.
-- Run this once on MySQL production databases that were created from the
-- earlier schema containing system_users.wecom_user_id.

ALTER TABLE system_users
    ADD COLUMN IF NOT EXISTS platform_user_id VARCHAR(100) NULL;

UPDATE system_users
SET platform_user_id = wecom_user_id
WHERE (platform_user_id IS NULL OR platform_user_id = '')
  AND wecom_user_id IS NOT NULL
  AND wecom_user_id != '';

UPDATE system_users
SET wecom_user_id = platform_user_id
WHERE (wecom_user_id IS NULL OR wecom_user_id = '')
  AND platform_user_id IS NOT NULL
  AND platform_user_id != '';

UPDATE system_users
SET platform_user_id = CONCAT('legacy_user_', id)
WHERE platform_user_id IS NULL OR platform_user_id = '';

UPDATE system_users
SET wecom_user_id = platform_user_id
WHERE wecom_user_id IS NULL OR wecom_user_id = '';

ALTER TABLE system_users
    MODIFY COLUMN platform_user_id VARCHAR(100) NOT NULL,
    MODIFY COLUMN wecom_user_id VARCHAR(100) NOT NULL;
