-- Seed data: 15 domestic routes across CGK/DPS/SUB/UPG/JOG/KNO, 12 hotels
-- spanning the same cities. Dates sit in the near future relative to project
-- development (Aug-Sep 2026) so search-by-date demos don't need "today" math.

INSERT INTO flights (flight_code, airline, origin, destination, departure_time, arrival_time, price, total_seats) VALUES
('GA400', 'Garuda Indonesia',  'CGK', 'DPS', '2026-08-01 06:00', '2026-08-01 08:55', 1850000, 72),
('JT782', 'Lion Air',          'CGK', 'DPS', '2026-08-01 09:30', '2026-08-01 12:20', 1150000, 72),
('GA401', 'Garuda Indonesia',  'DPS', 'CGK', '2026-08-03 14:00', '2026-08-03 16:45', 1900000, 72),
('QG602', 'Citilink',          'DPS', 'CGK', '2026-08-03 18:15', '2026-08-03 21:00', 1200000, 72),
('JT588', 'Lion Air',          'CGK', 'SUB', '2026-08-02 07:00', '2026-08-02 08:35', 980000,  72),
('ID6720','Batik Air',         'CGK', 'SUB', '2026-08-02 15:40', '2026-08-02 17:15', 1050000, 72),
('QG714', 'Citilink',          'SUB', 'CGK', '2026-08-04 10:00', '2026-08-04 11:35', 990000,  72),
('GA212', 'Garuda Indonesia',  'CGK', 'JOG', '2026-08-05 06:30', '2026-08-05 07:45', 890000,  72),
('IU728', 'Super Air Jet',     'CGK', 'JOG', '2026-08-05 13:10', '2026-08-05 14:25', 750000,  72),
('JT226', 'Lion Air',          'JOG', 'CGK', '2026-08-06 16:00', '2026-08-06 17:15', 820000,  72),
('GA660', 'Garuda Indonesia',  'CGK', 'UPG', '2026-08-07 05:45', '2026-08-07 08:50', 1650000, 72),
('ID6844','Batik Air',         'UPG', 'CGK', '2026-08-09 12:30', '2026-08-09 15:35', 1700000, 72),
('GA180', 'Garuda Indonesia',  'CGK', 'KNO', '2026-08-08 07:20', '2026-08-08 09:40', 1550000, 72),
('JT160', 'Lion Air',          'KNO', 'CGK', '2026-08-10 11:00', '2026-08-10 13:20', 1500000, 72),
('QG830', 'Citilink',          'DPS', 'SUB', '2026-08-11 09:00', '2026-08-11 10:20', 870000,  72);

-- 12 rows x 6 seats per flight; rows 1-2 BUSINESS, rows 3-12 ECONOMY.
INSERT INTO flight_seats (flight_id, seat_number, seat_class, status)
SELECT f.id,
       seat.row_num || seat.letter,
       CASE WHEN seat.row_num <= 2 THEN 'BUSINESS' ELSE 'ECONOMY' END,
       'AVAILABLE'
FROM flights f
CROSS JOIN (
    SELECT row_num, letter
    FROM generate_series(1, 12) AS row_num
    CROSS JOIN unnest(ARRAY['A','B','C','D','E','F']) AS letter
) seat;

INSERT INTO hotels (name, city, address, price_per_night, star_rating) VALUES
('Grand Inna Kuta',            'Bali',       'Jl. Pantai Kuta No. 1, Badung',        1200000, 4),
('Padma Resort Ubud',          'Bali',       'Jl. Raya Puhu, Payangan',              2500000, 5),
('Ibis Styles Bali Denpasar',  'Bali',       'Jl. Diponegoro No. 100, Denpasar',      650000, 3),
('Hotel Indonesia Kempinski',  'Jakarta',    'Jl. M.H. Thamrin No. 1',               2200000, 5),
('Ibis Jakarta Tamarin',       'Jakarta',    'Jl. K.H. Wahid Hasyim, Menteng',        550000, 3),
('The Phoenix Hotel Yogyakarta','Yogyakarta','Jl. Jenderal Sudirman No. 9',           950000, 4),
('Neo Malioboro',              'Yogyakarta', 'Jl. Malioboro, Sosromenduran',          480000, 3),
('Bumi Surabaya City Resort',  'Surabaya',   'Jl. Basuki Rahmat No. 106-128',        1350000, 5),
('Ibis Surabaya Jemursari',    'Surabaya',   'Jl. Jemursari No. 50',                  480000, 3),
('Claro Hotel Makassar',       'Makassar',   'Jl. A.P. Pettarani',                    900000, 4),
('Grand Aston City Hall Medan','Medan',      'Jl. Balai Kota No. 1',                 1050000, 4),
('Ibis Medan',                 'Medan',      'Jl. S. Parman',                         500000, 3);

-- 6 rooms per hotel: 101/102 Standard, 201/202 Deluxe, 301/302 Suite.
INSERT INTO hotel_rooms (hotel_id, room_number, room_type, status)
SELECT h.id, room.room_number, room.room_type, 'AVAILABLE'
FROM hotels h
CROSS JOIN (VALUES
    ('101', 'Standard'), ('102', 'Standard'),
    ('201', 'Deluxe'),   ('202', 'Deluxe'),
    ('301', 'Suite'),    ('302', 'Suite')
) AS room(room_number, room_type);
