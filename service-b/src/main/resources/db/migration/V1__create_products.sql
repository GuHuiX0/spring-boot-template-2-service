CREATE TABLE products (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name VARCHAR(120) NOT NULL CHECK (length(trim(name)) BETWEEN 1 AND 120),
    description VARCHAR(1000),
    price NUMERIC(19, 2) NOT NULL CHECK (price >= 0),
    stock_quantity INTEGER NOT NULL CHECK (stock_quantity >= 0),
    created_at VARCHAR(35) NOT NULL,
    updated_at VARCHAR(35) NOT NULL
);
