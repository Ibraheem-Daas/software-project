-- Insert Sample Data for Library Management System
-- Clean existing data first
TRUNCATE TABLE fine, loan, reservation, media_item, app_user RESTART IDENTITY CASCADE;

-- Insert Users (password is 'password123' for all)
INSERT INTO app_user (username, password, email, role, created_at) VALUES
('admin', 'password123', 'admin@library.com', 'ADMIN', NOW()),
('john_doe', 'password123', 'john@example.com', 'USER', NOW()),
('jane_smith', 'password123', 'jane@example.com', 'USER', NOW()),
('bob_jones', 'password123', 'bob@example.com', 'USER', NOW()),
('alice_wonder', 'password123', 'alice@example.com', 'USER', NOW());

-- Insert Media Items (Books)
INSERT INTO media_item (title, author, type, available_copies, total_copies, isbn, publication_date, publisher) VALUES
('Harry Potter and the Sorcerers Stone', 'J.K. Rowling', 'BOOK', 3, 5, '9780439708180', '1997-06-26', 'Scholastic'),
('The Hobbit', 'J.R.R. Tolkien', 'BOOK', 2, 3, '9780547928227', '1937-09-21', 'Allen & Unwin'),
('1984', 'George Orwell', 'BOOK', 4, 4, '9780451524935', '1949-06-08', 'Secker & Warburg'),
('To Kill a Mockingbird', 'Harper Lee', 'BOOK', 1, 2, '9780061120084', '1960-07-11', 'J.B. Lippincott'),
('The Great Gatsby', 'F. Scott Fitzgerald', 'BOOK', 3, 3, '9780743273565', '1925-04-10', 'Scribner'),
('Pride and Prejudice', 'Jane Austen', 'BOOK', 2, 2, '9780141439518', '1813-01-28', 'T. Egerton'),
('The Catcher in the Rye', 'J.D. Salinger', 'BOOK', 1, 1, '9780316769174', '1951-07-16', 'Little, Brown');

-- Insert Media Items (CDs)
INSERT INTO media_item (title, author, type, available_copies, total_copies, isbn, publication_date, publisher) VALUES
('Thriller', 'Michael Jackson', 'CD', 2, 3, 'CD001', '1982-11-30', 'Epic Records'),
('Abbey Road', 'The Beatles', 'CD', 1, 2, 'CD002', '1969-09-26', 'Apple Records'),
('Dark Side of the Moon', 'Pink Floyd', 'CD', 2, 2, 'CD003', '1973-03-01', 'Harvest'),
('Rumours', 'Fleetwood Mac', 'CD', 1, 1, 'CD004', '1977-02-04', 'Warner Bros');

-- Insert Media Items (DVDs)
INSERT INTO media_item (title, author, type, available_copies, total_copies, isbn, publication_date, publisher) VALUES
('The Shawshank Redemption', 'Frank Darabont', 'DVD', 2, 2, 'DVD001', '1994-09-23', 'Castle Rock'),
('The Godfather', 'Francis Ford Coppola', 'DVD', 1, 2, 'DVD002', '1972-03-24', 'Paramount'),
('Pulp Fiction', 'Quentin Tarantino', 'DVD', 3, 3, 'DVD003', '1994-10-14', 'Miramax'),
('Inception', 'Christopher Nolan', 'DVD', 2, 2, 'DVD004', '2010-07-16', 'Warner Bros');

-- Insert some active loans
INSERT INTO loan (user_id, item_id, loan_date, due_date, return_date, status) VALUES
(2, 1, CURRENT_DATE - INTERVAL '10 days', CURRENT_DATE + INTERVAL '18 days', NULL, 'ACTIVE'),
(3, 3, CURRENT_DATE - INTERVAL '5 days', CURRENT_DATE + INTERVAL '23 days', NULL, 'ACTIVE'),
(4, 8, CURRENT_DATE - INTERVAL '3 days', CURRENT_DATE + INTERVAL '4 days', NULL, 'ACTIVE');

-- Insert some returned loans
INSERT INTO loan (user_id, item_id, loan_date, due_date, return_date, status) VALUES
(2, 5, CURRENT_DATE - INTERVAL '35 days', CURRENT_DATE - INTERVAL '7 days', CURRENT_DATE - INTERVAL '5 days', 'RETURNED'),
(3, 7, CURRENT_DATE - INTERVAL '20 days', CURRENT_DATE - INTERVAL '6 days', CURRENT_DATE - INTERVAL '4 days', 'RETURNED');

-- Insert some overdue loans
INSERT INTO loan (user_id, item_id, loan_date, due_date, return_date, status) VALUES
(5, 2, CURRENT_DATE - INTERVAL '40 days', CURRENT_DATE - INTERVAL '12 days', NULL, 'ACTIVE');

-- Insert some fines
INSERT INTO fine (loan_id, amount, issued_date, paid_date, status) VALUES
(4, 20.00, CURRENT_DATE - INTERVAL '5 days', CURRENT_DATE - INTERVAL '2 days', 'PAID'),
(5, 40.00, CURRENT_DATE - INTERVAL '4 days', NULL, 'UNPAID'),
(6, 120.00, CURRENT_DATE - INTERVAL '12 days', NULL, 'UNPAID');

-- Insert some reservations
INSERT INTO reservation (user_id, item_id, reservation_date, expiry_date, status) VALUES
(2, 4, NOW() - INTERVAL '2 days', NOW() + INTERVAL '5 days', 'ACTIVE'),
(3, 12, NOW() - INTERVAL '1 day', NOW() + INTERVAL '6 days', 'ACTIVE'),
(4, 1, NOW(), NOW() + INTERVAL '7 days', 'ACTIVE');

-- Display summary
SELECT 'Sample data inserted successfully!' as message;
SELECT COUNT(*) as total_users FROM app_user;
SELECT COUNT(*) as total_media_items FROM media_item;
SELECT COUNT(*) as total_loans FROM loan;
SELECT COUNT(*) as total_fines FROM fine;
SELECT COUNT(*) as total_reservations FROM reservation;
