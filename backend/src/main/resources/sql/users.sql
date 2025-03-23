CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    birth_date DATE,
    gender VARCHAR(10),
    interests TEXT,
    city VARCHAR(100),
    username VARCHAR(100) UNIQUE,
    password VARCHAR(255)
)