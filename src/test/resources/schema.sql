-- Library Management System Database Schema
-- This schema is used by Testcontainers for integration testing

-- Drop tables if they exist (for clean state)
DROP TABLE IF EXISTS fine CASCADE;
DROP TABLE IF EXISTS loan CASCADE;
DROP TABLE IF EXISTS media_item CASCADE;
DROP TABLE IF EXISTS app_user CASCADE;

-- Create app_user table
CREATE TABLE app_user (
    user_id SERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    role VARCHAR(50) NOT NULL DEFAULT 'MEMBER',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create media_item table
CREATE TABLE media_item (
    item_id SERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    author VARCHAR(255),
    type VARCHAR(50) NOT NULL,
    isbn VARCHAR(20),
    publication_date DATE,
    publisher VARCHAR(255),
    total_copies INTEGER NOT NULL DEFAULT 1,
    available_copies INTEGER NOT NULL DEFAULT 1,
    late_fees_per_day DECIMAL(10, 2) NOT NULL DEFAULT 10.00,
    CONSTRAINT chk_copies CHECK (available_copies >= 0 AND available_copies <= total_copies)
);

-- Create loan table
CREATE TABLE loan (
    loan_id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    item_id INTEGER NOT NULL,
    loan_date DATE NOT NULL,
    due_date DATE NOT NULL,
    return_date DATE,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT fk_loan_user FOREIGN KEY (user_id) REFERENCES app_user(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_loan_item FOREIGN KEY (item_id) REFERENCES media_item(item_id) ON DELETE CASCADE,
    CONSTRAINT chk_dates CHECK (due_date >= loan_date)
);

-- Create fine table
CREATE TABLE fine (
    fine_id SERIAL PRIMARY KEY,
    loan_id INTEGER NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    issued_date DATE NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'UNPAID',
    paid_date DATE,
    CONSTRAINT fk_fine_loan FOREIGN KEY (loan_id) REFERENCES loan(loan_id) ON DELETE CASCADE,
    CONSTRAINT chk_amount CHECK (amount >= 0)
);

-- Create reservation table
CREATE TABLE reservation (
    reservation_id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    item_id INTEGER NOT NULL,
    reservation_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expiry_date TIMESTAMP NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT fk_reservation_user FOREIGN KEY (user_id) REFERENCES app_user(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_reservation_item FOREIGN KEY (item_id) REFERENCES media_item(item_id) ON DELETE CASCADE,
    CONSTRAINT chk_reservation_dates CHECK (expiry_date >= reservation_date),
    CONSTRAINT chk_reservation_status CHECK (status IN ('ACTIVE', 'FULFILLED', 'EXPIRED', 'CANCELLED'))
);

-- Create indexes for better query performance
CREATE INDEX idx_user_username ON app_user(username);
CREATE INDEX idx_user_email ON app_user(email);
CREATE INDEX idx_media_title ON media_item(title);
CREATE INDEX idx_media_type ON media_item(type);
CREATE INDEX idx_loan_user ON loan(user_id);
CREATE INDEX idx_loan_item ON loan(item_id);
CREATE INDEX idx_loan_status ON loan(status);
CREATE INDEX idx_fine_loan ON fine(loan_id);
CREATE INDEX idx_fine_status ON fine(status);
CREATE INDEX idx_reservation_user ON reservation(user_id);
CREATE INDEX idx_reservation_item ON reservation(item_id);
CREATE INDEX idx_reservation_status ON reservation(status);
CREATE INDEX idx_reservation_expiry ON reservation(expiry_date);
CREATE INDEX idx_reservation_item_status ON reservation(item_id, status, reservation_date);
