-- Dữ liệu demo V4 có thời điểm báo sự cố ở tương lai, khiến thời điểm hoàn thành hiện tại
-- vi phạm CK_SuCo_ThoiGian. Chỉ điều chỉnh các mã sự cố do V4 tạo.
UPDATE SuCo
SET ThoiDiemBao = DATE_SUB(
  UTC_TIMESTAMP(6),
  INTERVAL (1 + MOD(CAST(RIGHT(MaSuCo, 4) AS UNSIGNED), 30)) DAY
)
WHERE MaSuCo REGEXP '^SC(P)?[0-9]{4}$'
  AND ThoiDiemBao > UTC_TIMESTAMP(6)
  AND ThoiDiemHoanThanh IS NULL;
