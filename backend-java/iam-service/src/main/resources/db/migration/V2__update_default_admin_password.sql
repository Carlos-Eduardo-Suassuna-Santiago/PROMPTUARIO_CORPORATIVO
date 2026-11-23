-- Align the existing administrator with the documented default credential.
-- Password: Admin@12345
UPDATE users
SET hashed_password = '$2b$12$JUaOzDKc6TFgsUn04dRKC.GXf9IaN3WmULqfPutedEGf5waFFirHG'
WHERE email = 'admin@promptuario.health';
