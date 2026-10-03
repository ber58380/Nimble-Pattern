# 样板标记终端页签图标

使用内置 imagegen 生成透明 PNG，参考 `D:/mod/pattern_upgrade_terminal*.png`
的浅灰白图案、深色轮廓与像素箭头语言。未修改这些原始参考文件。

- 更新样板：`src/main/resources/assets/nimble_pattern/textures/guis/tabs/upgrade.png`，向上箭头与底横线。
- 循环样板：`src/main/resources/assets/nimble_pattern/textures/guis/tabs/loop.png`，首尾相接的两个箭头。
- 工具样板：继续使用原有下界合金升级锻造模板物品图标。

PNG 保留生成图的原始分辨率和透明通道；`TextureTabButton` 使用完整 UV 区域，
统一绘制为 16×16 GUI 像素，沿用 AE2 页签背景、选中效果和图标偏移。
材质包可在相同资源路径覆盖图标，无需注册新物品。

## 生成提示词（内置 imagegen，transparent_background=true）

### 更新样板

Use case: stylized-concept. Asset type: Minecraft mod GUI tab icon, transparent PNG. Create a single very simple pixel-art UPDATE PATTERN icon: one bold upward arrow rising from a short flat base bar. Match the previously shown old terminal textures: warm charcoal outline, off-white arrow face, a small amount of medium-gray lower/right pixel shading. This is a 16 by 16 logical pixel sprite, enlarged with nearest-neighbor square pixels if a larger image is required. Icon occupies central 12 by 12 logical pixels with 2 pixel transparent margin. Each logical pixel is one perfectly flat solid square, edges align to the same coarse 16x16 grid. Arrow tip centered at top, wide stepped triangular arrowhead and thick vertical stem, short separate base bar below. Strong simple silhouette recognizable at 16 screen pixels. Front facing flat UI sprite, not 3D. Entire background and margins genuinely transparent alpha. No text, letters, numbers, watermarks, gradients, glow, shadows outside the silhouette, anti-aliasing, scene, border frame, extra symbols or decorative details. Only one icon, not a sprite sheet.

### 循环样板

Use case: stylized-concept. Asset type: Minecraft mod GUI tab icon, transparent PNG. Create one LOOP PATTERN icon consisting of EXACTLY TWO thick clockwise arrows arranged head-to-tail around a small empty transparent square hole. Top arrow runs left-to-right across the top then turns down on the right; bottom arrow runs right-to-left across the bottom then turns up on the left. Keep two clear small gaps between the arrows so the two arrowheads are recognizable. Extremely simple bold stepped pixel-art sprite on a coarse 16 by 16 logical pixel grid, enlarge with nearest-neighbor square pixels if necessary. Central 12x12 logical pixels used, 2 pixel transparent margin. Same family as an upward update arrow: warm charcoal dark outlines, off-white flat faces, subtle medium-gray lower/right pixel shading. Intended to display at 16 screen pixels, every detail must remain readable that small. Front facing flat game UI, genuinely transparent alpha everywhere outside the two arrows and in the central hole. Hard axis-aligned square pixel steps, no smooth curves, no antialiasing, no textured surfaces, no glow or cast shadow, no text, no letters, no numbers, no watermark, no frame, no extra objects. Single centered icon, not a sheet.
