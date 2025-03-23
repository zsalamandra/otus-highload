-- Заполняем таблицу feeds данными о постах для лент пользователей
-- Для каждой пары дружбы (пользователь -> друг) добавляем последние посты друга в ленту пользователя

INSERT INTO feeds (user_id, post_id, created_at)
SELECT
    f.user_id,                      -- ID пользователя, чья лента пополняется
    p.id AS post_id,                -- ID поста друга
    NOW() - INTERVAL '1 minute' * RANDOM() * 1000  -- Случайное время добавления в ленту
FROM
    friendships f                   -- Берем все связи дружбы
JOIN
    posts p ON f.friend_id = p.user_id  -- Присоединяем посты, где автор - друг пользователя
-- Ограничиваем количество постов для каждого пользователя (не более 1000 последних от друзей)
WHERE
    p.id IN (
        SELECT p2.id
        FROM posts p2
        WHERE p2.user_id = f.friend_id
        ORDER BY p2.created_at DESC  -- Сортируем по времени создания
        LIMIT 1000                   -- Берем не более 1000 постов
    )
ORDER BY
    RANDOM()  -- Случайный порядок для разнообразия
LIMIT
    10000;    -- Общее ограничение на количество записей в feeds