INSERT INTO friendships (user_id, friend_id)
SELECT u1.id AS user_id, u2.id AS friend_id
FROM users u1
CROSS JOIN users u2
WHERE u1.id <> u2.id -- Исключаем случаи, когда user_id = friend_id
AND NOT EXISTS (
    SELECT 1 FROM friendships f
    WHERE f.user_id = u1.id AND f.friend_id = u2.id
)
ORDER BY
    -- Отдаем приоритет отношениям, где user_id = 1 или friend_id = 1
    CASE WHEN u1.id = 1 OR u2.id = 1 THEN 0 ELSE 1 END,
    RANDOM() -- Затем случайный порядок для остальных
LIMIT 50;