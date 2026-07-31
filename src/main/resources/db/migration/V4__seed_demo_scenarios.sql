-- Bộ dữ liệu demo tải lớn vừa phải cho máy cá nhân:
-- 300 thiết bị, 500 phiếu, lịch, phân bổ, phiên thực tế, sự cố và bảo trì.

-- 300 thiết bị phân bổ tuần tự qua 55 phòng và 10 loại thiết bị.
INSERT INTO
    ThietBi (
        MaThietBi,
        TenThietBi,
        MaLoai,
        SoSerial,
        Model,
        MaPhong,
        TrangThai
    )
WITH RECURSIVE
    seq AS (
        SELECT 1 AS n
        UNION ALL
        SELECT n + 1
        FROM seq
        WHERE
            n < 300
    )
SELECT
    CONCAT('TB', LPAD(n, 4, '0')),
    CONCAT(
        'Thiết bị demo ',
        LPAD(n, 4, '0')
    ),
    ELT(
        1 + MOD(n - 1, 10),
        'PC',
        'LAPTOP',
        'PROJECTOR',
        'ROBOT',
        'UAV',
        'SWITCH',
        'SERVER',
        'IOT',
        'CAMERA',
        'PRINTER3D'
    ),
    CONCAT('SERIAL-', LPAD(n, 6, '0')),
    CONCAT(
        'MODEL-',
        LPAD(1 + MOD(n - 1, 20), 2, '0')
    ),
    CONCAT(
        'P',
        LPAD(
            6 + FLOOR(MOD(n - 1, 55) / 5),
            2,
            '0'
        ),
        LPAD(1 + MOD(n - 1, 5), 2, '0')
    ),
    CASE
        WHEN n <= 270 THEN 'SAN_SANG'
        WHEN n <= 285 THEN 'BAO_TRI'
        WHEN n <= 295 THEN 'HONG'
        ELSE 'NGUNG_SU_DUNG'
    END
FROM seq;

INSERT INTO
    TaiNguyen (
        MaTaiNguyen,
        LoaiTaiNguyen,
        MaThietBi
    )
SELECT CONCAT('TN-', MaThietBi), 'THIET_BI', MaThietBi
FROM ThietBi;

-- 500 phiếu: 1/3 giảng dạy, còn lại học tập/nghiên cứu.
INSERT INTO
    PhieuDangKy (
        MaPhieu,
        MaNguoiTao,
        MaPhong,
        LoaiPhieu,
        MucDich,
        SoNguoi,
        NgayBatDau,
        NgayKetThuc,
        TrangThai
    )
WITH RECURSIVE
    seq AS (
        SELECT 1 AS n
        UNION ALL
        SELECT n + 1
        FROM seq
        WHERE
            n < 500
    )
SELECT
    CONCAT('PDK', LPAD(n, 4, '0')),
    CASE
        WHEN MOD(n, 3) = 0 THEN CONCAT(
            'GV',
            LPAD(1 + MOD(n - 1, 50), 3, '0')
        )
        ELSE CONCAT(
            'SV',
            LPAD(1 + MOD(n - 1, 200), 3, '0')
        )
    END,
    CONCAT(
        'P',
        LPAD(
            6 + FLOOR(MOD(n - 1, 55) / 5),
            2,
            '0'
        ),
        LPAD(1 + MOD(n - 1, 5), 2, '0')
    ),
    CASE
        WHEN MOD(n, 3) = 0 THEN 'GIANG_DAY'
        WHEN MOD(n, 3) = 1 THEN 'HOC_TAP'
        ELSE 'NGHIEN_CUU'
    END,
    CONCAT(
        'Mục đích sử dụng phòng và thiết bị số ',
        n
    ),
    1 + MOD(n * 7, 20),
    DATE_ADD(
        '2026-09-01',
        INTERVAL MOD(n - 1, 28) DAY
    ),
    DATE_ADD(
        '2026-12-15',
        INTERVAL MOD(n - 1, 10) DAY
    ),
    CASE
        WHEN MOD(n, 10) BETWEEN 0 AND 4  THEN 'DA_DUYET'
        WHEN MOD(n, 10) BETWEEN 5 AND 6  THEN 'CHO_DUYET'
        WHEN MOD(n, 10) = 7 THEN 'TU_CHOI'
        WHEN MOD(n, 10) = 8 THEN 'DA_HUY'
        ELSE 'HOAN_THANH'
    END
FROM seq;

INSERT INTO
    PhieuGiangDay (
        MaPhieu,
        MaHocPhan,
        TenLopNhom
    )
SELECT MaPhieu, CONCAT(
        'INT', LPAD(
            1 + MOD(
                CAST(
                    SUBSTRING(MaPhieu, 4) AS UNSIGNED
                ), 80
            ), 4, '0'
        )
    ), CONCAT(
        'N', LPAD(
            1 + MOD(
                CAST(
                    SUBSTRING(MaPhieu, 4) AS UNSIGNED
                ), 10
            ), 2, '0'
        )
    )
FROM PhieuDangKy
WHERE
    LoaiPhieu = 'GIANG_DAY';

-- Tất cả phiếu do sinh viên tạo đều có GVHD để bao phủ cả thiết bị kiểm soát.
INSERT INTO
    PhieuHuongDan (MaPhieu, MaGVHuongDan)
SELECT p.MaPhieu, CONCAT(
        'GV', LPAD(
            1 + MOD(
                CAST(
                    SUBSTRING(p.MaPhieu, 4) AS UNSIGNED
                ), 50
            ), 3, '0'
        )
    )
FROM PhieuDangKy p
    JOIN NguoiDung n ON n.MaNguoiDung = p.MaNguoiTao
WHERE
    n.MaVaiTro = 'SV';

-- Hai lịch/phiếu; dữ liệu cố ý có nhiều giao nhau để kiểm thử.
INSERT INTO
    LichDangKy (MaPhieu, Thu, MaTiet)
SELECT MaPhieu, 2 + MOD(
        CAST(
            SUBSTRING(MaPhieu, 4) AS UNSIGNED
        ), 6
    ), 1 + MOD(
        CAST(
            SUBSTRING(MaPhieu, 4) AS UNSIGNED
        ), 17
    )
FROM PhieuDangKy;

INSERT INTO
    LichDangKy (MaPhieu, Thu, MaTiet)
SELECT MaPhieu, 2 + MOD(
        CAST(
            SUBSTRING(MaPhieu, 4) AS UNSIGNED
        ) + 2, 6
    ), 1 + MOD(
        CAST(
            SUBSTRING(MaPhieu, 4) AS UNSIGNED
        ) + 5, 17
    )
FROM PhieuDangKy;

-- Hai thiết bị mong muốn/phiếu. Trạng thái DaPhanBo phản ánh phiếu đã duyệt/đã dùng.
INSERT INTO
    PhieuDangKyThietBi (MaPhieu, MaThietBi, DaPhanBo)
SELECT MaPhieu, CONCAT(
        'TB', LPAD(
            2 + 10 * MOD(
                CAST(
                    SUBSTRING(MaPhieu, 4) AS UNSIGNED
                ) - 1, 27
            ), 4, '0'
        )
    ), TrangThai IN (
        'DA_DUYET', 'DANG_SU_DUNG', 'HOAN_THANH'
    )
FROM PhieuDangKy;

INSERT INTO
    PhieuDangKyThietBi (MaPhieu, MaThietBi, DaPhanBo)
SELECT MaPhieu, CONCAT(
        'TB', LPAD(
            3 + 10 * MOD(
                CAST(
                    SUBSTRING(MaPhieu, 4) AS UNSIGNED
                ) - 1, 27
            ), 4, '0'
        )
    ), TrangThai IN (
        'DA_DUYET', 'DANG_SU_DUNG', 'HOAN_THANH'
    )
FROM PhieuDangKy;

-- Lịch sử quyết định cho mọi phiếu không còn chờ duyệt.
INSERT INTO
    XuLyPhieu (
        MaPhieu,
        MaNguoiXuLy,
        HanhDong,
        LyDo,
        ThoiDiem
    )
SELECT
    MaPhieu,
    CONCAT(
        'CB',
        LPAD(
            1 + MOD(
                CAST(
                    SUBSTRING(MaPhieu, 4) AS UNSIGNED
                ),
                10
            ),
            3,
            '0'
        )
    ),
    CASE
        WHEN TrangThai IN ('DA_DUYET', 'HOAN_THANH') THEN 'PHE_DUYET'
        WHEN TrangThai = 'TU_CHOI' THEN 'TU_CHOI'
        ELSE 'HUY'
    END,
    CASE
        WHEN TrangThai IN ('DA_DUYET', 'HOAN_THANH') THEN NULL
        WHEN TrangThai = 'TU_CHOI' THEN 'Không đáp ứng điều kiện sử dụng'
        ELSE 'Người dùng hủy phiếu'
    END,
    DATE_ADD(
        '2026-08-01 08:00:00',
        INTERVAL CAST(
            SUBSTRING(MaPhieu, 4) AS UNSIGNED
        ) HOUR
    )
FROM PhieuDangKy
WHERE
    TrangThai <> 'CHO_DUYET';

-- 40 lịch chặn, gồm chặn cả ngày và theo tiết.
INSERT INTO
    LichChan (
        MaTaiNguyen,
        NgayBatDau,
        NgayKetThuc,
        Thu,
        MaTiet,
        LyDo,
        TrangThai,
        MaNguoiTao
    )
WITH RECURSIVE
    seq AS (
        SELECT 1 AS n
        UNION ALL
        SELECT n + 1
        FROM seq
        WHERE
            n < 40
    )
SELECT
    CASE
        WHEN n <= 20 THEN CONCAT(
            'TN-P',
            LPAD(
                6 + FLOOR(MOD(n - 1, 55) / 5),
                2,
                '0'
            ),
            LPAD(1 + MOD(n - 1, 5), 2, '0')
        )
        ELSE CONCAT('TN-TB', LPAD(n, 4, '0'))
    END,
    DATE_ADD(
        '2026-09-01',
        INTERVAL MOD(n, 20) DAY
    ),
    DATE_ADD(
        '2026-09-07',
        INTERVAL MOD(n, 20) DAY
    ),
    CASE
        WHEN MOD(n, 3) = 0 THEN NULL
        ELSE 2 + MOD(n, 6)
    END,
    CASE
        WHEN MOD(n, 4) = 0 THEN NULL
        ELSE 1 + MOD(n, 17)
    END,
    CONCAT('Lịch chặn demo ', n),
    'HIEU_LUC',
    CONCAT(
        'CB',
        LPAD(1 + MOD(n - 1, 10), 3, '0')
    )
FROM seq;

-- Tối đa 100 phiên thực tế lấy từ các phiếu HOAN_THANH (hai lịch/phiếu).
INSERT INTO
    PhienSuDung (
        MaLich,
        NgaySuDung,
        TrangThai,
        ThoiDiemCheckIn,
        ThoiDiemCheckOut,
        MaNguoiCheckIn,
        MaNguoiCheckOut
    )
SELECT
    l.MaLich,
    DATE_ADD(
        '2026-09-01',
        INTERVAL MOD(l.MaLich, 28) DAY
    ),
    CASE
        WHEN MOD(l.MaLich, 10) = 0 THEN 'VANG_MAT'
        ELSE 'HOAN_THANH'
    END,
    CASE
        WHEN MOD(l.MaLich, 10) = 0 THEN NULL
        ELSE DATE_ADD(
            '2026-09-01 07:00:00',
            INTERVAL MOD(l.MaLich, 28) DAY
        )
    END,
    CASE
        WHEN MOD(l.MaLich, 10) = 0 THEN NULL
        ELSE DATE_ADD(
            '2026-09-01 08:40:00',
            INTERVAL MOD(l.MaLich, 28) DAY
        )
    END,
    CASE
        WHEN MOD(l.MaLich, 10) = 0 THEN NULL
        ELSE p.MaNguoiTao
    END,
    CASE
        WHEN MOD(l.MaLich, 10) = 0 THEN NULL
        ELSE p.MaNguoiTao
    END
FROM LichDangKy l
    JOIN PhieuDangKy p ON p.MaPhieu = l.MaPhieu
WHERE
    p.TrangThai = 'HOAN_THANH'
ORDER BY l.MaLich
LIMIT 200;

INSERT INTO
    PhienSuDungThietBi (
        MaPhien,
        MaThietBi,
        TinhTrangNhan,
        TinhTrangTra,
        GhiChu
    )
SELECT
    ps.MaPhien,
    pdtb.MaThietBi,
    'Tốt',
    CASE
        WHEN MOD(ps.MaPhien, 20) = 0 THEN 'Có dấu hiệu lỗi'
        ELSE 'Tốt'
    END,
    CONCAT(
        'Dữ liệu bàn giao phiên ',
        ps.MaPhien
    )
FROM
    PhienSuDung ps
    JOIN LichDangKy l ON l.MaLich = ps.MaLich
    JOIN PhieuDangKyThietBi pdtb ON pdtb.MaPhieu = l.MaPhieu
    AND pdtb.DaPhanBo = TRUE;

-- 50 sự cố độc lập; 25 sự cố đầu phát sinh bảo trì.
INSERT INTO
    SuCo (
        MaSuCo,
        MaTaiNguyen,
        MaPhien,
        MaNguoiBao,
        MaNguoiXuLy,
        MucDo,
        MoTa,
        TrangThai,
        ThoiDiemBao
    )
WITH RECURSIVE
    seq AS (
        SELECT 1 AS n
        UNION ALL
        SELECT n + 1
        FROM seq
        WHERE
            n < 50
    )
SELECT CONCAT('SC', LPAD(n, 4, '0')), CONCAT(
        'TN-TB', LPAD(1 + MOD(n * 5, 300), 4, '0')
    ), NULL, CONCAT(
        'SV', LPAD(1 + MOD(n - 1, 200), 3, '0')
    ), CONCAT(
        'CB', LPAD(1 + MOD(n - 1, 10), 3, '0')
    ), ELT(
        1 + MOD(n - 1, 4), 'THAP', 'TRUNG_BINH', 'CAO', 'NGHIEM_TRONG'
    ), CONCAT('Mô tả sự cố demo ', n), 'DANG_XU_LY', DATE_ADD(
        '2026-09-01 09:00:00', INTERVAL n DAY
    )
FROM seq;

-- Thêm tối đa 20 sự cố gắn đúng thiết bị thuộc phiên sử dụng thực tế.
INSERT INTO
    SuCo (
        MaSuCo,
        MaTaiNguyen,
        MaPhien,
        MaNguoiBao,
        MaNguoiXuLy,
        MucDo,
        MoTa,
        TrangThai,
        ThoiDiemBao
    )
SELECT CONCAT(
        'SCP', LPAD(ps.MaPhien, 4, '0')
    ), CONCAT('TN-', MIN(pstb.MaThietBi)), ps.MaPhien, COALESCE(ps.MaNguoiCheckIn, 'GV001'), CONCAT(
        'CB', LPAD(
            1 + MOD(ps.MaPhien - 1, 10), 3, '0'
        )
    ), 'TRUNG_BINH', CONCAT(
        'Sự cố phát sinh trong phiên ', ps.MaPhien
    ), 'DANG_XU_LY', DATE_ADD(
        ps.ThoiDiemCheckIn, INTERVAL 30 MINUTE
    )
FROM
    PhienSuDung ps
    JOIN PhienSuDungThietBi pstb ON pstb.MaPhien = ps.MaPhien
WHERE
    ps.TrangThai = 'HOAN_THANH'
GROUP BY
    ps.MaPhien,
    ps.MaNguoiCheckIn,
    ps.ThoiDiemCheckIn
ORDER BY ps.MaPhien
LIMIT 20;

INSERT INTO
    BaoTri (
        MaBaoTri,
        MaTaiNguyen,
        MaNguoiPhuTrach,
        NgayBatDau,
        NgayKetThuc,
        NoiDung,
        TrangThai,
        KetQua
    )
WITH RECURSIVE
    seq AS (
        SELECT 1 AS n
        UNION ALL
        SELECT n + 1
        FROM seq
        WHERE
            n < 25
    )
SELECT
    CONCAT('BT', LPAD(n, 4, '0')),
    CONCAT(
        'TN-TB',
        LPAD(1 + MOD(n * 5, 300), 4, '0')
    ),
    CONCAT(
        'CB',
        LPAD(1 + MOD(n - 1, 10), 3, '0')
    ),
    DATE_ADD(
        '2026-09-02 08:00:00',
        INTERVAL n DAY
    ),
    CASE
        WHEN MOD(n, 3) = 0 THEN DATE_ADD(
            '2026-09-04 17:00:00',
            INTERVAL n DAY
        )
        ELSE NULL
    END,
    CONCAT('Nội dung bảo trì demo ', n),
    CASE
        WHEN MOD(n, 3) = 0 THEN 'HOAN_THANH'
        ELSE 'DANG_BAO_TRI'
    END,
    CASE
        WHEN MOD(n, 3) = 0 THEN 'Đã khắc phục và kiểm tra hoạt động'
        ELSE NULL
    END
FROM seq;

INSERT INTO
    BaoTriSuCo (MaBaoTri, MaSuCo)
SELECT CONCAT('BT', LPAD(n, 4, '0')), CONCAT('SC', LPAD(n, 4, '0'))
FROM (
        SELECT 1 n
        UNION ALL
        SELECT 2
        UNION ALL
        SELECT 3
        UNION ALL
        SELECT 4
        UNION ALL
        SELECT 5
        UNION ALL
        SELECT 6
        UNION ALL
        SELECT 7
        UNION ALL
        SELECT 8
        UNION ALL
        SELECT 9
        UNION ALL
        SELECT 10
        UNION ALL
        SELECT 11
        UNION ALL
        SELECT 12
        UNION ALL
        SELECT 13
        UNION ALL
        SELECT 14
        UNION ALL
        SELECT 15
        UNION ALL
        SELECT 16
        UNION ALL
        SELECT 17
        UNION ALL
        SELECT 18
        UNION ALL
        SELECT 19
        UNION ALL
        SELECT 20
        UNION ALL
        SELECT 21
        UNION ALL
        SELECT 22
        UNION ALL
        SELECT 23
        UNION ALL
        SELECT 24
        UNION ALL
        SELECT 25
    ) numbers;

-- Ba dòng tiến độ cho mỗi bảo trì: tổng cộng 75 dòng.
INSERT INTO
    TienDoBaoTri (
        MaBaoTri,
        ThoiDiem,
        TrangThai,
        NoiDung,
        MaNguoiCapNhat
    )
SELECT b.MaBaoTri, b.NgayBatDau, 'CHO_XU_LY', 'Tiếp nhận yêu cầu bảo trì', b.MaNguoiPhuTrach
FROM BaoTri b;

INSERT INTO
    TienDoBaoTri (
        MaBaoTri,
        ThoiDiem,
        TrangThai,
        NoiDung,
        MaNguoiCapNhat
    )
SELECT b.MaBaoTri, DATE_ADD(b.NgayBatDau, INTERVAL 4 HOUR), 'DANG_BAO_TRI', 'Đang kiểm tra và sửa chữa', b.MaNguoiPhuTrach
FROM BaoTri b;

INSERT INTO
    TienDoBaoTri (
        MaBaoTri,
        ThoiDiem,
        TrangThai,
        NoiDung,
        MaNguoiCapNhat
    )
SELECT
    b.MaBaoTri,
    COALESCE(
        b.NgayKetThuc,
        DATE_ADD(b.NgayBatDau, INTERVAL 1 DAY)
    ),
    b.TrangThai,
    CASE
        WHEN b.TrangThai = 'HOAN_THANH' THEN 'Hoàn thành bảo trì'
        ELSE 'Tiếp tục xử lý'
    END,
    b.MaNguoiPhuTrach
FROM BaoTri b;