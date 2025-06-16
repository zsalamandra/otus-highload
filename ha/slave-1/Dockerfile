FROM postgres:14

# Копируем пользовательские конфиги
COPY postgresql.conf /etc/postgresql/postgresql.conf

# Указываем PostgreSQL использовать пользовательский конфиг
 CMD ["postgres", "-c", "config_file=/etc/postgresql/postgresql.conf"]
