-- Dữ liệu tham chiếu và danh mục cơ sở cho MySQL 9.4.
INSERT INTO VaiTro (MaVaiTro, TenVaiTro) VALUES
('CBQL','Cán bộ quản lý'),
('GV','Giảng viên'),
('SV','Sinh viên');

-- 17 tiết theo khung giờ cung cấp: sáng 1-6, chiều 7-12, tối 13-17.
INSERT INTO TietHoc (MaTiet, TenTiet, GioBatDau, GioKetThuc) VALUES
(1,'Tiết 1','07:00:00','07:50:00'),
(2,'Tiết 2','07:50:00','08:40:00'),
(3,'Tiết 3','08:40:00','09:30:00'),
(4,'Tiết 4','09:30:00','10:20:00'),
(5,'Tiết 5','10:20:00','11:10:00'),
(6,'Tiết 6','11:10:00','12:00:00'),
(7,'Tiết 7','12:30:00','13:20:00'),
(8,'Tiết 8','13:20:00','14:10:00'),
(9,'Tiết 9','14:10:00','15:00:00'),
(10,'Tiết 10','15:00:00','15:50:00'),
(11,'Tiết 11','15:50:00','16:40:00'),
(12,'Tiết 12','16:40:00','17:30:00'),
(13,'Tiết 13','17:45:00','18:35:00'),
(14,'Tiết 14','18:35:00','19:25:00'),
(15,'Tiết 15','19:25:00','20:15:00'),
(16,'Tiết 16','20:15:00','21:05:00'),
(17,'Tiết 17','21:05:00','21:55:00');

INSERT INTO NhomPhong (MaNhom, TenNhom, MoTa) VALUES
('NP01','Phòng máy','Phòng thực hành máy tính'),
('NP02','Phòng mạng','Phòng thực hành mạng và hệ thống'),
('NP03','Phòng nghiên cứu','Phòng robot, UAV và nghiên cứu chuyên ngành'),
('NP04','Phòng đa phương tiện','Phòng trình chiếu và học nhóm'),
('NP05','Phòng máy chủ','Phòng máy chủ và hạ tầng');

-- 11 tầng (6..16), mỗi tầng 5 phòng: tổng cộng 55 phòng.
INSERT INTO Phong (MaPhong, TenPhong, MaNhom, ViTri, SucChua, TrangThai)
WITH RECURSIVE floors AS (
  SELECT 6 AS floor_no
  UNION ALL
  SELECT floor_no + 1 FROM floors WHERE floor_no < 16
), room_slots AS (
  SELECT 1 AS slot_no UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
)
SELECT
  CONCAT('P', LPAD(floor_no, 2, '0'), LPAD(slot_no, 2, '0')),
  CONCAT('Phòng ', floor_no, '.', slot_no),
  CONCAT('NP0', slot_no),
  CONCAT('Tầng ', floor_no, ' - Phòng ', slot_no),
  CASE slot_no WHEN 1 THEN 40 WHEN 2 THEN 40 WHEN 3 THEN 30 WHEN 4 THEN 25 ELSE 20 END,
  'SAN_SANG'
FROM floors CROSS JOIN room_slots;

INSERT INTO LoaiThietBi (MaLoai, TenLoai, YeuCauGVHuongDan, LaThietBiDiDong, MoTa) VALUES
('PC','Máy tính',FALSE,FALSE,'Máy tính để bàn'),
('LAPTOP','Máy tính xách tay',FALSE,TRUE,'Máy tính dùng chung'),
('PROJECTOR','Máy chiếu',FALSE,TRUE,'Máy chiếu di động'),
('ROBOT','Robot',TRUE,TRUE,'Thiết bị yêu cầu giảng viên hướng dẫn'),
('UAV','UAV',TRUE,TRUE,'Thiết bị yêu cầu giảng viên hướng dẫn'),
('SWITCH','Thiết bị mạng',FALSE,TRUE,'Switch và router thực hành'),
('SERVER','Máy chủ',TRUE,FALSE,'Máy chủ phòng lab'),
('IOT','Bộ thiết bị IoT',TRUE,TRUE,'Kit cảm biến và vi điều khiển'),
('CAMERA','Camera',FALSE,TRUE,'Camera ghi hình thực hành'),
('PRINTER3D','Máy in 3D',TRUE,FALSE,'Máy in mô hình 3D');

-- Hash BCrypt mẫu chỉ dùng để kiểm tra cấu trúc; phải đặt lại mật khẩu trước demo thật.
-- 10 CBQL + 50 giảng viên + 200 sinh viên = 260 người dùng hoạt động.
INSERT INTO NguoiDung
(MaNguoiDung, HoTen, Email, MatKhau, LopDonVi, MaVaiTro, TrangThai)
WITH RECURSIVE seq AS (
  SELECT 1 AS n
  UNION ALL
  SELECT n + 1 FROM seq WHERE n < 260
)
SELECT
  CASE
    WHEN n <= 10 THEN CONCAT('CB', LPAD(n, 3, '0'))
    WHEN n <= 60 THEN CONCAT('GV', LPAD(n - 10, 3, '0'))
    ELSE CONCAT('SV', LPAD(n - 60, 3, '0'))
  END,
  CASE
    WHEN n <= 10 THEN CONCAT('Cán bộ quản lý ', LPAD(n, 2, '0'))
    WHEN n <= 60 THEN CONCAT('Giảng viên ', LPAD(n - 10, 2, '0'))
    ELSE CONCAT('Sinh viên ', LPAD(n - 60, 3, '0'))
  END,
  CASE
    WHEN n <= 10 THEN CONCAT('cb', LPAD(n, 3, '0'), '@lab.local')
    WHEN n <= 60 THEN CONCAT('gv', LPAD(n - 10, 3, '0'), '@lab.local')
    ELSE CONCAT('sv', LPAD(n - 60, 3, '0'), '@lab.local')
  END,
  '$2a$10$7EqJtq98hPqEX7fNZaFWoO5vZxYgYxJH0VZ1q3Vt8fLQqOe4vFQ8e',
  CASE
    WHEN n <= 60 THEN 'Khoa CNTT'
    ELSE CONCAT('CNTT', LPAD(1 + MOD(n - 61, 10), 2, '0'))
  END,
  CASE WHEN n <= 10 THEN 'CBQL' WHEN n <= 60 THEN 'GV' ELSE 'SV' END,
  'HOAT_DONG'
FROM seq;

-- Mỗi phòng có đúng một tài nguyên đại diện.
INSERT INTO TaiNguyen (MaTaiNguyen, LoaiTaiNguyen, MaPhong)
SELECT CONCAT('TN-', MaPhong), 'PHONG', MaPhong FROM Phong;
