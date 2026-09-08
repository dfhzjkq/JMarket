ALTER TABLE users
ADD COLUMN password_hash VARCHAR(255);

UPDATE users
SET password_hash = '$2b$12$R8Qm4WnVx0fEcYvS2G7mE.3JbH1eA9xXz6kKcLmNpQrStUvWxYz12'
WHERE password_hash IS NULL;

ALTER TABLE users
ALTER COLUMN password_hash SET NOT NULL;
