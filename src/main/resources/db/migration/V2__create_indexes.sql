CREATE INDEX IX_NguoiDung_TrangThaiVaiTro ON NguoiDung (TrangThai, MaVaiTro);
CREATE INDEX IX_Phong_NhomTrangThai ON Phong (MaNhom, TrangThai);
CREATE INDEX IX_ThietBi_LoaiPhongTrangThai ON ThietBi (MaLoai, MaPhong, TrangThai);
CREATE INDEX IX_Phieu_NguoiTrangThai ON PhieuDangKy (MaNguoiTao, TrangThai, NgayTao);
CREATE INDEX IX_Phieu_PhongNgayTrangThai ON PhieuDangKy (MaPhong, NgayBatDau, NgayKetThuc, TrangThai);
CREATE INDEX IX_Lich_ThuTietPhieu ON LichDangKy (Thu, MaTiet, MaPhieu);
CREATE INDEX IX_PDKTB_ThietBiPhieu ON PhieuDangKyThietBi (MaThietBi, MaPhieu, DaPhanBo);
CREATE INDEX IX_XuLy_PhieuThoiDiem ON XuLyPhieu (MaPhieu, ThoiDiem);
CREATE INDEX IX_LichChan_TaiNguyenNgay ON LichChan (MaTaiNguyen, TrangThai, NgayBatDau, NgayKetThuc, Thu, MaTiet);
CREATE INDEX IX_Phien_NgayTrangThai ON PhienSuDung (NgaySuDung, TrangThai);
CREATE INDEX IX_SuCo_TaiNguyenTrangThai ON SuCo (MaTaiNguyen, TrangThai, ThoiDiemBao);
CREATE INDEX IX_BaoTri_TaiNguyenTrangThai ON BaoTri (MaTaiNguyen, TrangThai, NgayBatDau);
CREATE INDEX IX_TienDo_BaoTriThoiDiem ON TienDoBaoTri (MaBaoTri, ThoiDiem);

