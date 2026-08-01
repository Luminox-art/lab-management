ALTER TABLE LichChan
  ADD COLUMN MaBaoTri VARCHAR(50) NULL AFTER MaNguoiTao,
  ADD CONSTRAINT UK_LichChan_BaoTri UNIQUE (MaBaoTri),
  ADD CONSTRAINT FK_LichChan_BaoTri FOREIGN KEY (MaBaoTri) REFERENCES BaoTri(MaBaoTri);

INSERT INTO LichChan (
  MaTaiNguyen,
  NgayBatDau,
  NgayKetThuc,
  Thu,
  MaTiet,
  LyDo,
  TrangThai,
  MaNguoiTao,
  MaBaoTri
)
SELECT
  bt.MaTaiNguyen,
  DATE(bt.NgayBatDau),
  '9999-12-31',
  NULL,
  NULL,
  CONCAT('Bảo trì: ', bt.NoiDung),
  'HIEU_LUC',
  bt.MaNguoiPhuTrach,
  bt.MaBaoTri
FROM BaoTri bt
WHERE bt.TrangThai IN ('CHO_XU_LY', 'DANG_BAO_TRI');
