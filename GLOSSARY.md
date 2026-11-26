# HuashijieLibre Digital Art Terminology Glossary

Tài liệu này khóa chuẩn thuật ngữ hội họa số cho toàn bộ gói ngôn ngữ của **HuashijieLibre** (bộ patch cộng đồng dành cho **画世界Pro** v3.4.6), tham chiếu trực tiếp từ `lang/_template_zh.json`, `lang/en.json` và `lang/vi.json`. Người dịch và người phụ trách ngôn ngữ (`Language Maintainer`) đối chiếu bảng này để giữ thống nhất tên công cụ giữa thanh công cụ, bảng lớp, trình chỉnh cọ và hộp thoại xuất tệp.

## 1. Quy ước tên gọi và nguyên tắc nhãn ngắn

1. **Tên dự án (`HuashijieLibre`):** Luôn viết liền không dấu cách là `HuashijieLibre` (không tách rời `Huashijie` và `Libre`).
2. **Tên ứng dụng gốc (`画世界Pro`):** Giữ nguyên là `画世界Pro` trong chuỗi giao diện (trừ các chuỗi thuộc tính đường dẫn tệp nội bộ; không chèn dấu cách trước `Pro`).
3. **Thư mục dữ liệu trên Android (`HuashijiePro`):** Đường dẫn `/storage/emulated/0/HuashijiePro/` (và các khóa bảo vệ `0x7f110803` `sdcard_root_folder` = `"HuashijiePro"`, `0x7f110077` `album_folder` = `"画世界Pro"`) tuyệt đối không dịch.
4. **Ưu tiên nhãn ngắn gọn cho thanh công cụ:** Các nút biểu tượng, tab bảng điều khiển và tiêu đề thanh trượt có bề ngang hẹp. Chọn từ vựng chuẩn của phần mềm vẽ (Krita, Clip Studio Paint, Procreate) và giữ độ dài ngắn nhất có thể để tránh xuống dòng hoặc cắt chữ; giữ đồng nhất bản dịch khi cùng một chuỗi nguồn xuất hiện ở cả `strings_by_id` và `strings_by_text`.

## 2. Không gian làm việc, bản nháp và Canvas

| Tiếng Trung gốc | Tiếng Anh | Tiếng Việt | Ghi chú |
| :--- | :--- | :--- | :--- |
| `画布` | Canvas | Canvas | Giữ ngắn gọn; ưu tiên dùng trực tiếp `Canvas` trong tiếng Việt cho nhãn hẹp. |
| `草稿箱` | Drafts | Bản nháp | Thư viện dự án cục bộ tại màn hình chính. |
| `草稿` | Draft | Bản nháp | Tệp dự án `.hsj`. |
| `新建画布` | New Canvas | Canvas mới | Hộp thoại tạo trang vẽ mới. |
| `像素` | Pixel / Pixels | Pixel | Đơn vị kích thước canvas và cọ. |
| `旋转` | Rotate | Xoay | Thao tác xoay góc nhìn hoặc đối tượng. |
| `翻转画布` | Flip Canvas | Lật Canvas | Lật toàn bộ khung nhìn bản vẽ. |
| `水平翻转` | Flip Horizontal | Lật ngang | Lật theo trục ngang. |
| `垂直翻转` | Flip Vertical | Lật dọc | Lật theo trục dọc. |
| `裁剪空画布` | Trim Empty Canvas | Cắt phần trống Canvas | Loại bỏ vùng rìa trong suốt quanh nội dung vẽ. |
| `参考图层` | Reference Layer | Tham chiếu | Đặt lớp làm nguồn tham chiếu khi tô thùng sơn hoặc chọn vùng. |
| `参考图片` | Reference Image | Hình ảnh tham chiếu | Ảnh mẫu trong cửa sổ tham chiếu. |
| `对称` | Symmetry | Đối xứng | Thước vẽ đối xứng trục hoặc tâm. |
| `辅助线` | Drawing Guide | Đường gióng | Công cụ hỗ trợ căn chỉnh và phối cảnh. |
| `网格` | Grid | Lưới | Lưới căn chỉnh trên canvas. |

## 3. Lớp (Layer), Mặt nạ (Mask) và Chế độ hòa trộn (Blend Mode)

| Tiếng Trung gốc | Tiếng Anh | Tiếng Việt | Ghi chú |
| :--- | :--- | :--- | :--- |
| `图层` | Layer | Layer | Dùng thống nhất `Layer` (ngắn hơn "Lớp") trên thanh bên hẹp. |
| `图层组` | Layer Group | Nhóm Layer | Thư mục gom nhiều layer. |
| `新建图层` | New Layer | Tạo Layer mới | Thêm layer trống mới. |
| `复制图层` | Duplicate Layer | Nhân đôi Layer | Tạo bản sao của layer đang chọn. |
| `合并图层` | Merge Layers | Gộp Layer | Nhập các layer được chọn thành một. |
| `画布盖印` | Stamp Canvas | Gộp khung vẽ | Gộp các layer đang hiển thị thành một layer mới ở trên cùng. |
| `锁定透明度` | Alpha Lock | Khóa Alpha | Khóa vùng điểm ảnh trong suốt trên layer hiện tại. |
| `锁定图层` | Lock Layer | Khóa Layer | Chống chỉnh sửa nhầm lên layer. |
| `剪贴蒙版` / `剪切蒙版` | Clipping Mask | Clipping Mask | Cắt hiển thị theo hình dáng layer bên dưới. |
| `图层蒙版` | Layer Mask | Mặt nạ Layer | Mặt nạ che vùng hiển thị không phá hủy nét gốc. |
| `不透明度` | Opacity | Độ mờ | Dùng ngắn gọn `Độ mờ` trên thanh trượt; tránh kéo dài thành `Độ mờ đục` trên nhãn hẹp. |
| `混合模式` | Blend Mode | Chế độ hòa trộn | Nhóm chế độ trộn màu giữa các layer. |
| `正常` | Normal | Bình thường | Chế độ hòa trộn mặc định của layer. |
| `正片叠底` | Multiply | Nhân | Hòa trộn làm tối màu, hay dùng cho đổ bóng. |
| `滤色` | Screen | Lọc màu | Hòa trộn làm sáng màu, đảo ngược của Multiply. |
| `叠加` | Overlay | Phủ lên | Kết hợp Multiply và Screen giữ tương phản. |
| `柔光` | Soft Light | Ánh sáng dịu | Tăng giảm sáng dịu theo độ sáng nền. |
| `强光` | Hard Light | Ánh sáng gắt | Tăng giảm sáng tương phản mạnh. |
| `变暗` | Darken | Làm tối | Giữ giá trị kênh màu tối hơn. |
| `变亮` | Lighten | Làm sáng | Giữ giá trị kênh màu sáng hơn. |
| `颜色减淡` | Color Dodge | Làm sáng màu | Tăng sáng rực và giảm tương phản tối. |
| `颜色加深` | Color Burn | Cháy màu | Làm tối sâu và tăng độ bão hòa vùng tối. |
| `线性减淡` | Linear Dodge | Sáng tuyến tính | Cộng sáng trực tiếp (Add/Linear Dodge). |
| `线性加深` | Linear Burn | Tối tuyến tính | Trừ tối tuyến tính. |
| `亮光` | Vivid Light | Ánh sáng chói | Kết hợp Color Dodge và Color Burn. |
| `线性光` | Linear Light | Ánh sáng tuyến tính | Kết hợp Linear Dodge và Linear Burn. |
| `点光` | Pin Light | Ánh sáng điểm | Thay thế màu theo ngưỡng sáng trung tính. |
| `实色混合` | Hard Mix | Trộn màu đặc | Đẩy màu về các mức cơ bản có độ gắt cao. |
| `差值` | Difference | Sai biệt | Lấy hiệu tuyệt đối giữa hai lớp màu. |
| `排除` | Exclusion | Loại trừ | Tương tự Difference nhưng dịu tương phản hơn. |
| `减去` | Subtract | Trừ | Trừ giá trị màu lớp trên khỏi lớp dưới. |
| `划分` | Divide | Chia | Chia giá trị màu lớp dưới cho lớp trên. |
| `色相` | Hue | Sắc độ | Áp dụng tông màu của lớp trên. |
| `饱和度` | Saturation | Độ bão hòa | Áp dụng độ tươi màu của lớp trên. |
| `颜色` | Color | Màu sắc | Áp dụng cả Hue và Saturation của lớp trên. |
| `明度` | Brightness | Độ sáng | Áp dụng độ sáng/tối (Luminosity/Brightness). |

## 4. Cọ vẽ (Brush), Tô màu và Tham số nét

| Tiếng Trung gốc | Tiếng Anh | Tiếng Việt | Ghi chú |
| :--- | :--- | :--- | :--- |
| `画笔` / `笔刷` | Brush | Cọ vẽ | Dùng thống nhất `Brush` (số ít) ở nhãn nút công cụ và `Brushes` khi chỉ danh sách chung. |
| `笔刷库` | Brush Library | Thư viện cọ | Cửa sổ quản lý toàn bộ nhóm cọ. |
| `柳叶笔` | Willow Pen | Bút Lá Liễu | Cọ tô kín hình khép kín đặc trưng của 画世界Pro (trên nhãn ngắn giữ `Willow Pen` hoặc `Bút Lá Liễu`). |
| `橡皮擦` | Eraser | Tẩy | Công cụ xóa nét vẽ. |
| `涂抹` | Smudge | Smudge | Công cụ di màu / làm nhòe nét; giữ ngắn gọn trên thanh công cụ. |
| `液化` | Liquify | Liquify | Công cụ đẩy/kéo biến dạng điểm ảnh trực tiếp. |
| `吸管` | Eyedropper | Ống nhỏ giọt | Công cụ hút màu từ canvas. |
| `油漆桶` | Paint Bucket | Thùng sơn | Công cụ tô kín vùng màu khép kín. |
| `填充` | Fill | Tô | Thao tác tô màu vào vùng chọn hoặc layer. |
| `渐变` | Gradient | Chuyển sắc | Công cụ tạo dải chuyển màu. |
| `文字` | Text | Văn bản | Công cụ chèn và chỉnh sửa chữ. |
| `色卡` | Color Palette | Bảng màu | Tập hợp ô màu lưu sẵn. |
| `大小` | Size | Kích thước | Đường kính nét cọ (giữ ngắn cho thanh trượt). |
| `流量` | Flow | Lưu lượng | Mức độ phun màu tích lũy trên mỗi bước nét. |
| `间距` | Spacing | Khoảng cách | Khoảng cách giữa các dấu ấn đầu cọ. |
| `硬度` | Hardness | Độ cứng | Độ sắc nét của viền đầu cọ. |
| `抖动修正` | Stabilization | Ổn định nét | Bộ lọc chống rung tay khi đi nét. |

## 5. Vùng chọn (Selection), Biến đổi, Hoạt họa và Xuất tệp

| Tiếng Trung gốc | Tiếng Anh | Tiếng Việt | Ghi chú |
| :--- | :--- | :--- | :--- |
| `选区` | Selection | Vùng chọn | Khu vực được khoanh vùng để chỉnh sửa riêng. |
| `套索` | Lasso | Lasso | Công cụ khoanh vùng tự do bằng nét vẽ. |
| `魔棒` | Magic Wand | Đũa thần | Công cụ chọn vùng có màu tương đồng. |
| `矩形选区` | Rectangular Selection | Vùng chọn Hình chữ nhật | Công cụ tạo vùng chọn chữ nhật. |
| `取消选择` | Deselect | Bỏ chọn | Thoát vùng chọn đang kích hoạt. |
| `反选` | Invert | Đảo vùng chọn | Đảo ngược phần trong và ngoài vùng chọn. |
| `变形` | Warp | Biến dạng | Uốn cong lưới biến đổi tự do. |
| `扭曲` | Distort | Bóp méo | Kéo lệch các góc biến đổi độc lập. |
| `透视图` | Perspective | Phối cảnh | Nhóm lưới phối cảnh (`一点透视` 1-Point, `二点透视` 2-Point, `三点透视` 3-Point). |
| `撤销` | Undo | Hoàn tác | Quay lại bước thao tác trước. |
| `重做` | Redo | Làm lại | Khôi phục bước vừa hoàn tác. |
| `动画` | Animation | Hoạt họa | Chế độ vẽ hoạt hình theo khung hình. |
| `洋葱皮` | Onion Skin | Onion Skin | Hiển thị mờ khung hình trước/sau khi diễn hoạt. |
| `帧率` | Frame Rate | Tốc độ khung hình | Số khung hình trên giây (FPS). |
| `导出` | Export | Xuất | Xuất ảnh, PSD, PDF, `.hsj` hoặc video. |
| `导出设置` | Export settings | Cài đặt xuất | Tùy chọn chất lượng và định dạng khi xuất tệp. |
| `系统分享` | Share | Chia sẻ | Mở bảng chia sẻ chuẩn của Android (`Android Share Sheet`). |
