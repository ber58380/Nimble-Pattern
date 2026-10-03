# 09 逐文件职责、改动与内容指纹

基准 HEAD：`df96c5cbb2e8752cf57fad1676a40d507aece4cd`。下表快照于 2026-10-03 整理，涵盖业务源码、所有主/生成资源、测试、删除文件及相关构建和既有说明。**本目录新撰写的审核文档自身不参与内容哈希清单**，以避免自引用；见文末职责表。

统计：180 项（M：34；??：61；=：78；D：7）。`M`=相对 HEAD 修改；`??`=新增/未跟踪内容；`=`=当前无差异；`D`=已删除。未跟踪不等于刚刚创建。`+/-` 为已跟踪文本的 Git 行数统计，不含未跟踪文件的内容。

每项先说明当前职责，再说明相对基线变化。实现算法、异常路径和限制在 01–08；不要只凭文件名判断支持范围。链接使用仓库相对路径，审核文档移交时请保留目录结构。SHA-256 对工作区原始字节计算，换行转换也会改变它。

## 构建、许可和既有文档

### 1. .gitignore

文件：[.gitignore](../../.gitignore)。

状态：`M`；文本差异 +2/-0；402 字节。

职责：忽略构建/IDE/运行环境、生成器缓存和测试日志，避免将测试世界与庞大日志提交。

改动：相对 HEAD 增加 /run-gametest/ 与 /optimization-test.log；其他忽略项原已存在。

内容指纹：`810c39305f4bcf8e6d6402a2c85b420ec19c2010510d9d9f9881bf27985b7f33`。

### 2. build.gradle

文件：[build.gradle](../../build.gradle)。

状态：`M`；文本差异 +19/-2；10576 字节。

职责：ForgeGradle/Mixin 构建、Java 17、运行依赖、生成资源、reobf 打包、GameTest 启动与结构复制。

改动：GameTest 独立 run-gametest 目录并加入 sourceSets.test；按任务名跳过客户端专用 JECharacters；启用 JEI runtime；新增 prepareToolTestStructures 和 GameTest 依赖。

内容指纹：`004f26e52b412966f09a160ac8369a42aadb586fca1e50ca4ca0851e019d7f98`。

### 3. optimization.md

文件：[docs/optimization.md](../../docs/optimization.md)。

状态：`??`；4361 字节。

职责：此前优化摘要及 32 GameTest 成功记录；细节由本目录 06/08 补齐。

改动：基准中不存在，当前为未跟踪文件；新增内容为上述实现。

内容指纹：`b3d1a5270dcc2c23cc8519b670d08aa1a9a5b01114ec50025c6af66fee1a405f`。

### 4. probability-patterns.md

文件：[docs/probability-patterns.md](../../docs/probability-patterns.md)。

状态：`??`；3701 字节。

职责：概率玩家说明，支持范围、基础概率、手调数量/删输入、机器与超时、补料/取消。

改动：基准中不存在，当前为未跟踪文件；新增内容为上述实现。

内容指纹：`e3841536eab2e68d8b306dbe6a2be6714f78295016304399b34ab0668299685b`。

### 5. tab-icons.md

文件：[docs/tab-icons.md](../../docs/tab-icons.md)。

状态：`??`；3272 字节。

职责：内置 imagegen 模式、两张透明页签图标路径、显示大小、最终生成提示词；便于美术来源追溯。

改动：基准中不存在，当前为未跟踪文件；新增内容为上述实现。

内容指纹：`c122ec76c7ec53e2688973412f681c33228dc9989389b3c36fd44407332d38b5`。

### 6. gradle.properties

文件：[gradle.properties](../../gradle.properties)。

状态：`=`；517 字节。

职责：精确开发版本和模组元信息、JVM 参数、映射版本；当前 AE 15.4.10/Forge 47.4.16/MC 1.20.1。

改动：与比较基准无差异；保留上述既有实现，纳入整体审核，不虚构本轮修改。

内容指纹：`eb9afafc02ec31f3ac00c842407cdc0dce8082e4264cb45a3b1e6f1166f686e0`。

### 7. gradle-wrapper.jar

文件：[gradle/wrapper/gradle-wrapper.jar](../../gradle/wrapper/gradle-wrapper.jar)。

状态：`=`；43453 字节。

职责：Gradle wrapper 引导二进制，不是模组运行内容；审核可校验 SHA-256 与可信分发。

改动：与比较基准无差异；保留上述既有实现，纳入整体审核，不虚构本轮修改。

内容指纹：`cb0da6751c2b753a16ac168bb354870ebb1e162e9083f116729cec9c781156b8`。

### 8. gradle-wrapper.properties

文件：[gradle/wrapper/gradle-wrapper.properties](../../gradle/wrapper/gradle-wrapper.properties)。

状态：`=`；257 字节。

职责：固定 Gradle 8.8 分发 URL、超时和缓存位置；用于跨开发机一致构建。

改动：与比较基准无差异；保留上述既有实现，纳入整体审核，不虚构本轮修改。

内容指纹：`053fecb8aafc90218106cefdebbfc8c04f92c1b124d3d0f0a454e358c0118dfc`。

### 9. gradlew

文件：[gradlew](../../gradlew)。

状态：`=`；8955 字节。

职责：Unix Gradle wrapper 启动脚本；依赖 Java 环境，用于 Linux/macOS 构建。

改动：与比较基准无差异；保留上述既有实现，纳入整体审核，不虚构本轮修改。

内容指纹：`94335111924844294fb3da35f634fff07d4405fdebd5a7d78662ff801546b99e`。

### 10. gradlew.bat

文件：[gradlew.bat](../../gradlew.bat)。

状态：`=`；2918 字节。

职责：Windows Gradle wrapper 启动脚本；PowerShell/CMD 均可调用。

改动：与比较基准无差异；保留上述既有实现，纳入整体审核，不虚构本轮修改。

内容指纹：`bdecf875b6868cbcbd36a1f85eedf0832f358ff28092c5797ed645f7edce77d9`。

### 11. LICENSE

文件：[LICENSE](../../LICENSE)。

状态：`=`；43637 字节。

职责：仓库许可证正文；不是业务修改。代码与材质复用须同时保留原项目相应许可/署名。

改动：与比较基准无差异；保留上述既有实现，纳入整体审核，不虚构本轮修改。

内容指纹：`1d85626ef75ce27566e00d818619374d1013547e040657b1eb0c56547a6aae22`。

### 12. README.md

文件：[README.md](../../README.md)。

状态：`M`；文本差异 +11/-1；3483 字节。

职责：英文玩家/项目介绍，功能和许可链接；不是严格实现说明。

改动：新增概率/工具功能说明，指向概率中文文档；保留原模糊/假合成介绍。

内容指纹：`b88d5383dcaffd23bccf6766ea25e878b12dbdc525cde5cf132696eba3d91d2b`。

### 13. README.zh.md

文件：[README.zh.md](../../README.zh.md)。

状态：`M`；文本差异 +13/-1；3374 字节。

职责：中文玩家/项目介绍，含工具使用约束、概率模式和原有功能。

改动：新增概率及工具功能/一次预留说明，更新末尾换行；未代替此次逐文件审核说明。

内容指纹：`e20c93f4e83f231c5d0393b5f67a96121c087e9de864a2792861f478e6a1b38a`。

### 14. settings.gradle

文件：[settings.gradle](../../settings.gradle)。

状态：`=`；386 字节。

职责：Gradle 插件解析/工程设置入口；不实现游戏运行功能。

改动：与比较基准无差异；保留上述既有实现，纳入整体审核，不虚构本轮修改。

内容指纹：`2f11c9d789814dd2e97b5f129ff7e28f82dfe4d1ab88e4136dce0ba2528c3301`。

## 生成资源

### 15. en_us.json

文件：[src/generated/resources/assets/nimble_pattern/lang/en_us.json](../../src/generated/resources/assets/nimble_pattern/lang/en_us.json)。

状态：`M`；文本差异 +30/-0；4666 字节。

职责：ModEnUsLangProvider 生成的英文运行文案；审核源 provider 和产物键同步。

改动：追加 30 个工具/循环/概率相关翻译键。

内容指纹：`b3a6b9d15cf2af51f8b52bfedc2b8a3a2c80afb75dbd0ba6f9aabd55ee7ac83b`。

### 16. zh_cn.json

文件：[src/generated/resources/assets/nimble_pattern/lang/zh_cn.json](../../src/generated/resources/assets/nimble_pattern/lang/zh_cn.json)。

状态：`M`；文本差异 +30/-0；4499 字节。

职责：ModZhCnLangProvider 生成的中文运行文案；审核占位符与包内 tooltip/toast 调用一致。

改动：追加 30 个工具/循环/概率相关翻译键。

内容指纹：`2cead7a66b716cc9033b9155372fc09e5ae92869a53c4d77d62456dc3602a8d0`。

### 17. loop_storage_cell_16k.json

文件：[src/generated/resources/assets/nimble_pattern/models/item/loop_storage_cell_16k.json](../../src/generated/resources/assets/nimble_pattern/models/item/loop_storage_cell_16k.json)。

状态：`=`；121 字节。

职责：16k 循环元件的生成物品模型，parent=item/generated，layer0 指向同名 nimble_pattern:item 纹理；枚举改造保持资源 ID 不变。

改动：与比较基准无差异；保留上述既有实现，纳入整体审核，不虚构本轮修改。

内容指纹：`fb1a916053ffad2689722166989cf22a2d81003f42c8f824c0f0954b701fcd22`。

### 18. loop_storage_cell_1k.json

文件：[src/generated/resources/assets/nimble_pattern/models/item/loop_storage_cell_1k.json](../../src/generated/resources/assets/nimble_pattern/models/item/loop_storage_cell_1k.json)。

状态：`=`；120 字节。

职责：1k 循环元件的生成物品模型，parent=item/generated，layer0 指向同名 nimble_pattern:item 纹理；枚举改造保持资源 ID 不变。

改动：与比较基准无差异；保留上述既有实现，纳入整体审核，不虚构本轮修改。

内容指纹：`fb67a9ea15b8da6c428c8d665da5586a55acb6d4cf961e3574062af88263d8a6`。

### 19. loop_storage_cell_256k.json

文件：[src/generated/resources/assets/nimble_pattern/models/item/loop_storage_cell_256k.json](../../src/generated/resources/assets/nimble_pattern/models/item/loop_storage_cell_256k.json)。

状态：`=`；122 字节。

职责：256k 循环元件的生成物品模型，parent=item/generated，layer0 指向同名 nimble_pattern:item 纹理；枚举改造保持资源 ID 不变。

改动：与比较基准无差异；保留上述既有实现，纳入整体审核，不虚构本轮修改。

内容指纹：`9c034afe4555d7eb7c7199a11662309a89493455960cdab5c5a5d258138dc901`。

### 20. loop_storage_cell_4k.json

文件：[src/generated/resources/assets/nimble_pattern/models/item/loop_storage_cell_4k.json](../../src/generated/resources/assets/nimble_pattern/models/item/loop_storage_cell_4k.json)。

状态：`=`；120 字节。

职责：4k 循环元件的生成物品模型，parent=item/generated，layer0 指向同名 nimble_pattern:item 纹理；枚举改造保持资源 ID 不变。

改动：与比较基准无差异；保留上述既有实现，纳入整体审核，不虚构本轮修改。

内容指纹：`2737656805fd815941b8b568ff65c8cfe1a278b4609a276f09dc4b2962c39939`。

### 21. loop_storage_cell_64k.json

文件：[src/generated/resources/assets/nimble_pattern/models/item/loop_storage_cell_64k.json](../../src/generated/resources/assets/nimble_pattern/models/item/loop_storage_cell_64k.json)。

状态：`=`；121 字节。

职责：64k 循环元件的生成物品模型，parent=item/generated，layer0 指向同名 nimble_pattern:item 纹理；枚举改造保持资源 ID 不变。

改动：与比较基准无差异；保留上述既有实现，纳入整体审核，不虚构本轮修改。

内容指纹：`0180f69aabbefed10f36c81bbe9ddd8e926974f7a668c2b37e10c06184451106`。

### 22. pattern_tag_terminal.json

文件：[src/generated/resources/data/nimble_pattern/advancements/recipes/misc/pattern_tag_terminal.json](../../src/generated/resources/data/nimble_pattern/advancements/recipes/misc/pattern_tag_terminal.json)。

状态：`=`；681 字节。

职责：终端配方解锁进度，取得 AE 样板管理终端后解锁对应配方；由 datagen 产生。

改动：与比较基准无差异；保留上述既有实现，纳入整体审核，不虚构本轮修改。

内容指纹：`1d7895784e9ac3b90221eaffd685f34859fa566671e184bcf2633142f28ea67a`。

### 23. pattern_tag_terminal.json

文件：[src/generated/resources/data/nimble_pattern/recipes/pattern_tag_terminal.json](../../src/generated/resources/data/nimble_pattern/recipes/pattern_tag_terminal.json)。

状态：`=`；319 字节。

职责：ModRecipesProvider 生成终端无序合成配方，玩家实际获得终端的入口。

改动：与比较基准无差异；保留上述既有实现，纳入整体审核，不虚构本轮修改。

内容指纹：`bcf167f7d0b37249403e2e68f2e5ec349a8db8bbe6471a9cd0be169151cb03d0`。

## 业务 Java 源码

### 24. ClientModEvents.java

文件：[src/main/java/com/ber/nimblePattern/client/ClientModEvents.java](../../src/main/java/com/ber/nimblePattern/client/ClientModEvents.java)。

状态：`=`；1716 字节。

职责：客户端注册 PatternTagTermMenu→PatternTagTermScreen 及 AE StyleManager 布局；终端物品颜色使用 AEColor.TRANSPARENT。只在 Dist.CLIENT 加载。

改动：与比较基准无差异；保留上述既有实现，纳入整体审核，不虚构本轮修改。

内容指纹：`b4e4f24d1b1839d68cd854034d8185c63043e56d155228472f9d990cee5f4ac7`。

### 25. PatternLoopPanel.java

文件：[src/main/java/com/ber/nimblePattern/client/gui/panel/PatternLoopPanel.java](../../src/main/java/com/ber/nimblePattern/client/gui/panel/PatternLoopPanel.java)。

状态：`M`；文本差异 +30/-4；2616 字节。

职责：循环页材质与应用按钮；updateBeforeRender 定位 LOOP_INPUT/OUTPUT/LOCK_STORAGE_CELL，setVisible 一起切换三个槽及按钮；创建循环纹理页签。

改动：用 NimbleButton 新增 applyLoop 按钮及隐藏联动；定位循环端点/元件槽；旧 MC 物品图标改 TextureTabButton 的 loop.png。

内容指纹：`1a5dbb92d8ed411e7fb9635fea7ec7cf46e48a013521e993bed4bc56b21cf907`。

### 26. PatternToolPanel.java

文件：[src/main/java/com/ber/nimblePattern/client/gui/panel/PatternToolPanel.java](../../src/main/java/com/ber/nimblePattern/client/gui/panel/PatternToolPanel.java)。

状态：`??`；2191 字节。

职责：工具页材质、四个 TOOL_INPUT 的定位/隐藏和 applyTools 按钮；页签保留下界合金升级锻造模板物品，不使用自定义 PNG。

改动：基准中不存在，当前为未跟踪文件；新增内容为上述实现。

内容指纹：`fe556f87a280d05f680d412c08b17c019a037595540f588be89e231aed5094ec`。

### 27. PatternUpgradePanel.java

文件：[src/main/java/com/ber/nimblePattern/client/gui/panel/PatternUpgradePanel.java](../../src/main/java/com/ber/nimblePattern/client/gui/panel/PatternUpgradePanel.java)。

状态：`M`；文本差异 +5/-3；6101 字节。

职责：更新页材质、条件输入框和条件 FakeSlot 同步（syncTextAndSlot 防递归），历史补全、clear/apply；物品条件取注册 ID，文字条件去空白；创建更新纹理页签。

改动：getTabIconItem 改为 createTabButton，FURNACE 占位图标替换 upgrade.png；更新条件表单逻辑保留。

内容指纹：`94bb9be2d975bde50ff4784029783be68eda6833f25600e6a547ed0629ab2d74`。

### 28. TagModePanel.java

文件：[src/main/java/com/ber/nimblePattern/client/gui/panel/TagModePanel.java](../../src/main/java/com/ber/nimblePattern/client/gui/panel/TagModePanel.java)。

状态：`M`；文本差异 +3/-2；1571 字节。

职责：ICompositeWidget 抽象基类，保存 screen/menu/widgets、局部 x/y 和 visible；88×68 bounds；提供 createTabButton(OnPress)/getTabTooltip，具体页只画变化部分。

改动：图标抽象接口从返回 ItemStack 改为创建 TabButton，允许各页使用纹理或物品而无需 Screen 知道类型。

内容指纹：`7172c7d3aaa073a29d0302fdec72e3ecfada023dcc50fbfa5d6b245408d31f27`。

### 29. PatternContainerRecord.java

文件：[src/main/java/com/ber/nimblePattern/client/gui/PatternContainerRecord.java](../../src/main/java/com/ber/nimblePattern/client/gui/PatternContainerRecord.java)。

状态：`=`；554 字节。

职责：客户端每个临时 serverId 对应的 AppEngInternalInventory 镜像；供网络包更新和远程槽显示，不是服务端真实库存。

改动：与比较基准无差异；保留上述既有实现，纳入整体审核，不虚构本轮修改。

内容指纹：`c47705fad327aba6e0f42a2afce85c43ba24f09dfa57ba22d20e47372c2f95ca`。

### 30. PatternRecord.java

文件：[src/main/java/com/ber/nimblePattern/client/gui/PatternRecord.java](../../src/main/java/com/ber/nimblePattern/client/gui/PatternRecord.java)。

状态：`=`；517 字节。

职责：扁平化浏览记录：serverId、机器槽号、样板栈、主产物名称的小写 sortKey；避免排序比较器反复提取显示名。

改动：与比较基准无差异；保留上述既有实现，纳入整体审核，不虚构本轮修改。

内容指纹：`7b45b08365d19d2ba2970bd4735441d8eab9fe901426dac255f7e55734e79cc9`。

### 31. PatternTagTermScreen.java

文件：[src/main/java/com/ber/nimblePattern/client/gui/PatternTagTermScreen.java](../../src/main/java/com/ber/nimblePattern/client/gui/PatternTagTermScreen.java)。

状态：`M`；文本差异 +17/-5；13857 字节。

职责：终端公共画面和样板浏览：网络列表、搜索/排序、全量与增量记录、81 个共享输入槽的可见行、两条滚动条；按 TagMode 建 panel/tab，控制选中和显示；viewDirty/slotsDirty 合并刷新。

改动：构造器新增 TOOL panel，页签创建改为 panel.createTabButton；增量更新置 viewDirty，在渲染前合并重建；slotsDirty/scroll/rows 防重复重建。共享样板槽和滚动逻辑在 HEAD 已存在，不把它重复记成本轮新增。

内容指纹：`0f71aab514d5b478ba9c596dc6eca34f7e6c04df493d1fbe56bb6b0dee1ceb6b`。

### 32. AndSearchPredicate.java

文件：[src/main/java/com/ber/nimblePattern/client/gui/search/AndSearchPredicate.java](../../src/main/java/com/ber/nimblePattern/client/gui/search/AndSearchPredicate.java)。

状态：`=`；924 字节。

职责：组合 AND 条件并短路；空列表为恒真，单项直接返回原 predicate，减少无意义组合对象。

改动：与比较基准无差异；保留上述既有实现，纳入整体审核，不虚构本轮修改。

内容指纹：`659ccaf6c8fc83ccdafe599d4b559da2b0d337f5e58337c52ef8aa56c055fca8`。

### 33. ConditionSearchPredicate.java

文件：[src/main/java/com/ber/nimblePattern/client/gui/search/ConditionSearchPredicate.java](../../src/main/java/com/ber/nimblePattern/client/gui/search/ConditionSearchPredicate.java)。

状态：`=`；2179 字节。

职责：% 条件搜索：读 NimblePatternTag.getCondition，将可识别注册 ID 转可翻译 Component 并缓存，支持子串及可选拼音。

改动：与比较基准无差异；保留上述既有实现，纳入整体审核，不虚构本轮修改。

内容指纹：`30e2e6777867805e81daa3200e670317acf68c909ae696590d4b712324d60168`。

### 34. ItemIdSearchPredicate.java

文件：[src/main/java/com/ber/nimblePattern/client/gui/search/ItemIdSearchPredicate.java](../../src/main/java/com/ber/nimblePattern/client/gui/search/ItemIdSearchPredicate.java)。

状态：`=`；750 字节。

职责：* 搜索主产物 AEKey 的完整注册 ID，比较小写字符串；不是对样板物品自身 ID 搜索。

改动：与比较基准无差异；保留上述既有实现，纳入整体审核，不虚构本轮修改。

内容指纹：`7d9adc0681dc45338068a99c1607278e76db20bb3815bed8db79c3218569497c`。

### 35. ModSearchPredicate.java

文件：[src/main/java/com/ber/nimblePattern/client/gui/search/ModSearchPredicate.java](../../src/main/java/com/ber/nimblePattern/client/gui/search/ModSearchPredicate.java)。

状态：`=`；1018 字节。

职责：@ 搜索主产物模组 ID 或 Platform.getModName 名称；处理 null key/模组 ID。

改动：与比较基准无差异；保留上述既有实现，纳入整体审核，不虚构本轮修改。

内容指纹：`cbdc0fedd39bb081fefbf7c999057bbe9867ef0887b10eaade00213572aed7ba`。

### 36. NameSearchPredicate.java

文件：[src/main/java/com/ber/nimblePattern/client/gui/search/NameSearchPredicate.java](../../src/main/java/com/ber/nimblePattern/client/gui/search/NameSearchPredicate.java)。

状态：`=`；735 字节。

职责：主产物显示名小写子串匹配，可通过 PinInHelper 增加拼音匹配。

改动：与比较基准无差异；保留上述既有实现，纳入整体审核，不虚构本轮修改。

内容指纹：`976fdb641905850d7f2ac1a63fde2fe855fc399366d960687257bf9fa3fc43c4`。

### 37. OrSearchPredicate.java

文件：[src/main/java/com/ber/nimblePattern/client/gui/search/OrSearchPredicate.java](../../src/main/java/com/ber/nimblePattern/client/gui/search/OrSearchPredicate.java)。

状态：`=`；908 字节。

职责：组合 OR 条件并短路；空列表为恒假，单项直接返回。供竖线分隔的多组查询使用。

改动：与比较基准无差异；保留上述既有实现，纳入整体审核，不虚构本轮修改。

内容指纹：`f0cbb6c043add2a283c73250022ff0ed96c59a2cb627a2f8afacf89e401b5dbb`。

### 38. PatternSearch.java

文件：[src/main/java/com/ber/nimblePattern/client/gui/search/PatternSearch.java](../../src/main/java/com/ber/nimblePattern/client/gui/search/PatternSearch.java)。

状态：`=`；2591 字节。

职责：只在 query 改变时构建谓词树；空格 AND、竖线 OR，分派 @/#/$/*/%/~ 前缀；保存 tooltip 弱键缓存。

改动：与比较基准无差异；保留上述既有实现，纳入整体审核，不虚构本轮修改。

内容指纹：`4ae856257d56c856e5c58cdce631c420e418c24fa61da51f9729d15e7f7bc00d`。

### 39. StatusSearchPredicate.java

文件：[src/main/java/com/ber/nimblePattern/client/gui/search/StatusSearchPredicate.java](../../src/main/java/com/ber/nimblePattern/client/gui/search/StatusSearchPredicate.java)。

状态：`=`；1095 字节。

职责：~ 搜索 UNTRACKED/0、LATEST/1、UPDATE/2；未知字符串转 null，不匹配任何正常状态。

改动：与比较基准无差异；保留上述既有实现，纳入整体审核，不虚构本轮修改。

内容指纹：`75c3a205202cfc8efa528977820afef6c56e250d6fb8e1e8b7f941a9b26b9dfc`。

### 40. TagSearchPredicate.java

文件：[src/main/java/com/ber/nimblePattern/client/gui/search/TagSearchPredicate.java](../../src/main/java/com/ber/nimblePattern/client/gui/search/TagSearchPredicate.java)。

状态：`=`；1943 字节。

职责：$ 搜索 AEKey tags，按 AEKeyType 缓存符合名称的 TagKey 列表；再对每个主产物调用 isTagged。

改动：与比较基准无差异；保留上述既有实现，纳入整体审核，不虚构本轮修改。

内容指纹：`d711c72e6ae7a25eff68bbd5f994cae905c6dbfd1bc3fd3430f61b13a9cfedd9`。

### 41. TooltipsSearchPredicate.java

文件：[src/main/java/com/ber/nimblePattern/client/gui/search/TooltipsSearchPredicate.java](../../src/main/java/com/ber/nimblePattern/client/gui/search/TooltipsSearchPredicate.java)。

状态：`=`；3122 字节。

职责：# 搜索主产物 tooltip，去格式/空格和统一大小写，按 AE 配置处理末尾模组名；弱键缓存与可选拼音。

改动：与比较基准无差异；保留上述既有实现，纳入整体审核，不虚构本轮修改。

内容指纹：`3b20fd3171a171818ab423cc35221baf01e45a7231174c3d8f72f1ef8066ac9c`。

### 42. UnwrapHelper.java

文件：[src/main/java/com/ber/nimblePattern/client/gui/search/UnwrapHelper.java](../../src/main/java/com/ber/nimblePattern/client/gui/search/UnwrapHelper.java)。

状态：`=`；1822 字节。

职责：从 EncodedPatternItem.getOutput 取得主产物，解开 GenericStack 流体包装，提供 AEKey/显示名给搜索和排序。

改动：与比较基准无差异；保留上述既有实现，纳入整体审核，不虚构本轮修改。

内容指纹：`3d883b7f48a3b72669b15eb505a5b2f136d140136dd036fcd8741475f7f301c3`。

### 43. LoopSeedLostToast.java

文件：[src/main/java/com/ber/nimblePattern/client/gui/widgets/LoopSeedLostToast.java](../../src/main/java/com/ber/nimblePattern/client/gui/widgets/LoopSeedLostToast.java)。

状态：`??`；2522 字节。

职责：绘制 AEKey 图标、可换行标题/内容，显示 5 秒；可传自定义翻译键，所以同时承载概率失败 toast。

改动：基准中不存在，当前为未跟踪文件；新增内容为上述实现。

内容指纹：`acc6f5697f0d70473716dfd017d7a81de757b3a3eb7db57208d1b6c7d8f34413`。

### 44. NimbleButton.java

文件：[src/main/java/com/ber/nimblePattern/client/gui/widgets/NimbleButton.java](../../src/main/java/com/ber/nimblePattern/client/gui/widgets/NimbleButton.java)。

状态：`=`；3261 字节。

职责：自定义应用/清除按钮：三态 PNG、2 像素边缘九宫格拉伸、居中文字、禁用颜色和焦点边框；每次绘制创建多个 Blitter。

改动：与比较基准无差异；保留上述既有实现，纳入整体审核，不虚构本轮修改。

内容指纹：`08dbf4b3e9fc6430a429f662bd062380fca928bad778aa5d23ba4e42aad8650f`。

### 45. PatternUpgradeSlot.java

文件：[src/main/java/com/ber/nimblePattern/client/gui/widgets/PatternUpgradeSlot.java](../../src/main/java/com/ber/nimblePattern/client/gui/widgets/PatternUpgradeSlot.java)。

状态：`=`；1920 字节。

职责：远程样板显示槽：绑定 serverId/machineSlot，客户端优先显示产物；禁止原生 set/remove/mayPickup，真实动作由菜单协议执行。

改动：与比较基准无差异；保留上述既有实现，纳入整体审核，不虚构本轮修改。

内容指纹：`dac37a322663bbc325d5b5496ca3f7e827672a77e531e2292e874d77ec6f8adc`。

### 46. PatternUpgradeToast.java

文件：[src/main/java/com/ber/nimblePattern/client/gui/widgets/PatternUpgradeToast.java](../../src/main/java/com/ber/nimblePattern/client/gui/widgets/PatternUpgradeToast.java)。

状态：`=`；2683 字节。

职责：更新条件通知，显示条件名称/可选物品或流体图标、匹配样板数；可变高度、2.5 秒显示。

改动：与比较基准无差异；保留上述既有实现，纳入整体审核，不虚构本轮修改。

内容指纹：`cef6a69c3d1eff09edec3729ae838eabe735ac83dd31365a744fec838584deb6`。

### 47. PromptTextField.java

文件：[src/main/java/com/ber/nimblePattern/client/gui/widgets/PromptTextField.java](../../src/main/java/com/ber/nimblePattern/client/gui/widgets/PromptTextField.java)。

状态：`=`；2630 字节。

职责：条件输入补全：从历史中找前缀候选，绘制灰色后缀；Tab 补全后移至末尾，过长输入不显示提示。

改动：与比较基准无差异；保留上述既有实现，纳入整体审核，不虚构本轮修改。

内容指纹：`fe32a2ce7a9e98a03c6698ab8709fda0f9c8436c8a38908717eacc64cf893653`。

### 48. TextureTabButton.java

文件：[src/main/java/com/ber/nimblePattern/client/gui/widgets/TextureTabButton.java](../../src/main/java/com/ber/nimblePattern/client/gui/widgets/TextureTabButton.java)。

状态：`??`；1373 字节。

职责：继承 AE TabButton 保留背景和 tooltip，独立 Blitter 以完整 UV 画 16×16 图标；按 CORNER/BOX/HORIZONTAL 保持 AE 偏移。

改动：基准中不存在，当前为未跟踪文件；新增内容为上述实现。

内容指纹：`1a16aaded47593990f4b40f4d7cc085dfb2d8499f5449cf8cf29a2a7a48ff5be`。

### 49. ExtendedAECompat.java

文件：[src/main/java/com/ber/nimblePattern/compat/extendedae/ExtendedAECompat.java](../../src/main/java/com/ber/nimblePattern/compat/extendedae/ExtendedAECompat.java)。

状态：`=`；980 字节。

职责：检测 expatternprovider 模组；封装 PartExPatternProvider 的 side 和 terminal inventory 访问，供来源定位使用。

改动：与比较基准无差异；保留上述既有实现，纳入整体审核，不虚构本轮修改。

内容指纹：`76fb70bfa278c634d5c7918ab9314f42cdfafc60becd7bbf49e55f119a7bc0f4`。

### 50. GtlBatchAdapter.java

文件：[src/main/java/com/ber/nimblePattern/compat/gtl/GtlBatchAdapter.java](../../src/main/java/com/ber/nimblePattern/compat/gtl/GtlBatchAdapter.java)。

状态：`??`；1377 字节。

职责：ClassValue<Optional<Method>> 缓存可选 GTL 最大批数方法，以 ProviderPatternIndex.nativePattern 查询；不存在/反射异常时退回 1。

改动：基准中不存在，当前为未跟踪文件；新增内容为上述实现。

内容指纹：`4f320445eba3a4562899c53d72ea04e36073973b6eaee090a5e795456aadc6d3`。

### 51. PinInHelper.java

文件：[src/main/java/com/ber/nimblePattern/compat/jecharacters/PinInHelper.java](../../src/main/java/com/ber/nimblePattern/compat/jecharacters/PinInHelper.java)。

状态：`=`；568 字节。

职责：检测 jecharacters，延迟初始化 PinIn，contains 为搜索提供可选拼音；未安装直接 false。

改动：与比较基准无差异；保留上述既有实现，纳入整体审核，不虚构本轮修改。

内容指纹：`f95115b0e11c2f3b4e8b9a2038c4e9e63e41298f3077a720fe94e012613b646e`。

### 52. Config.java

文件：[src/main/java/com/ber/nimblePattern/Config.java](../../src/main/java/com/ber/nimblePattern/Config.java)。

状态：`M`；文本差异 +3/-0；885 字节。

职责：ForgeConfigSpec 定义概率未知机器等待秒数 probabilityPatternTimeoutSeconds（120，1..86400）；onLoad 目前为空，运行端直接读取 IntValue。

改动：新增概率超时 IntValue 配置及范围；其余配置框架保持。

内容指纹：`7e45450d8047ee5a2b84c2d93fb30034dd46219a4261cb2222adf99d5e646566`。

### 53. FuzzyOutputLedger.java

文件：[src/main/java/com/ber/nimblePattern/crafting/FuzzyOutputLedger.java](../../src/main/java/com/ber/nimblePattern/crafting/FuzzyOutputLedger.java)。

状态：`??`；3125 字节。

职责：模糊产物候选账本：dropSecondary→精确 key/量；优先精确收货，同步清索引；按 AE 实际欠量接受变体；SIMULATE 不写；save/load/clear。

改动：基准中不存在，当前为未跟踪文件；新增内容为上述实现。

内容指纹：`9f641f282603c3011a4d8fd23155bb7c6756e2626593876548a7e9ed31b72fd9`。

### 54. LoopBatchDispatch.java

文件：[src/main/java/com/ber/nimblePattern/crafting/LoopBatchDispatch.java](../../src/main/java/com/ber/nimblePattern/crafting/LoopBatchDispatch.java)。

状态：`??`；847 字节。

职责：ThreadLocal 保存 GTL pattern 对象与操作数，take 按引用核对并立即删除，默认 1；用于正确传递循环/概率真实批数。

改动：基准中不存在，当前为未跟踪文件；新增内容为上述实现。

内容指纹：`3f2494a5e5a52ef3091b7045369edb05964bf01ae6151ef1ab47ac9b70134a16`。

### 55. LoopBatchSizing.java

文件：[src/main/java/com/ber/nimblePattern/crafting/LoopBatchSizing.java](../../src/main/java/com/ber/nimblePattern/crafting/LoopBatchSizing.java)。

状态：`??`；540 字节。

职责：纯算术 next=min(remaining,availableSeed/seedPerOperation)，验证非负输入和正种子需求；不实际派发机器或改库存。

改动：基准中不存在，当前为未跟踪文件；新增内容为上述实现。

内容指纹：`733d2b76243f3c708ae9c843276fbafd34c5e5897495e6c31ecba7ce33fd57ba`。

### 56. LoopCraftingController.java

文件：[src/main/java/com/ber/nimblePattern/crafting/LoopCraftingController.java](../../src/main/java/com/ber/nimblePattern/crafting/LoopCraftingController.java)。

状态：`??`；14481 字节。

职责：活动环状态机：解析步骤和指定元件、取种子、working 暂存、按现有种子扩批、顺序投料、精确主产物收齐后推进、归种后交付净收益、取消回收；当前无 NBT save/load。

改动：基准中不存在，当前为未跟踪文件；新增内容为上述实现。

内容指纹：`0064cfe532ae264af79110ed21813899ae2b7b146895ec547530433a6addc3f8`。

### 57. ToolCraftingController.java

文件：[src/main/java/com/ber/nimblePattern/crafting/ToolCraftingController.java](../../src/main/java/com/ber/nimblePattern/crafting/ToolCraftingController.java)。

状态：`??`；5777 字节。

职责：reserved/borrowed/expected/received 四库存控制工具真实借还；完整网格投料，拒绝回滚，输出和余物齐全才发布结果；取消释放真实持有，保存恢复全部账本。

改动：基准中不存在，当前为未跟踪文件；新增内容为上述实现。

内容指纹：`4a2f65ee0354cb8ebb99608a7edc9e48e17cbb627f62c2df69a1a21aa5919cf1`。

### 58. ToolCraftingPlan.java

文件：[src/main/java/com/ber/nimblePattern/crafting/ToolCraftingPlan.java](../../src/main/java/com/ber/nimblePattern/crafting/ToolCraftingPlan.java)。

状态：`??`；1941 字节。

职责：从 patternTimes 按 key 最大单次量计算工具预留，用网络快照调整 usedItems/missingItems/simulation/bytes，避免工具随配方次数倍增。

改动：基准中不存在，当前为未跟踪文件；新增内容为上述实现。

内容指纹：`25d317c0f5bff1496e590a4fe18fe962c05cf2a764006963ba467f5ff399ea07`。

### 59. ModBlockTagsProvider.java

文件：[src/main/java/com/ber/nimblePattern/datagen/ModBlockTagsProvider.java](../../src/main/java/com/ber/nimblePattern/datagen/ModBlockTagsProvider.java)。

状态：`=`；773 字节。

职责：方块标签生成入口，目前 addTags 为空；不是已经定义了循环/工具材料分类标签。

改动：与比较基准无差异；保留上述既有实现，纳入整体审核，不虚构本轮修改。

内容指纹：`1f99159c39d04e4c5965b3499daeb2cb5fcce000790b82043866bc92d89298ba`。

### 60. ModEnUsLangProvider.java

文件：[src/main/java/com/ber/nimblePattern/datagen/ModEnUsLangProvider.java](../../src/main/java/com/ber/nimblePattern/datagen/ModEnUsLangProvider.java)。

状态：`M`；文本差异 +30/-0；5662 字节。

职责：英文物品名、终端文案、搜索说明、循环/工具校验结果、概率标记与提醒的生成源；修改后需 runData 同步 en_us.json。

改动：增加概率/工具文案、循环解析各错误、取消丢种提醒；相应生成语言文件同改。

内容指纹：`8cee4fec2debd9d2d4a0633d722d810bb8666cebd71b611183fdb734297f7c7f`。

### 61. ModItemModelsProvider.java

文件：[src/main/java/com/ber/nimblePattern/datagen/ModItemModelsProvider.java](../../src/main/java/com/ber/nimblePattern/datagen/ModItemModelsProvider.java)。

状态：`=`；1783 字节。

职责：为五种元件生成 item/generated + 单 layer0 贴图模型；仍显式调用五个注册常量，basicItem 提供命名空间/路径映射。

改动：与比较基准无差异；保留上述既有实现，纳入整体审核，不虚构本轮修改。

内容指纹：`6f70e3fb4c56ebc6d67766530dac7dad9ca1342b2e83ece4c45054a354f3c861`。

### 62. ModItemTagsProvider.java

文件：[src/main/java/com/ber/nimblePattern/datagen/ModItemTagsProvider.java](../../src/main/java/com/ber/nimblePattern/datagen/ModItemTagsProvider.java)。

状态：`=`；875 字节。

职责：物品标签生成入口，目前 addTags 为空；依赖 block provider 的 contentsGetter。

改动：与比较基准无差异；保留上述既有实现，纳入整体审核，不虚构本轮修改。

内容指纹：`9fb527389a0f83f72908952b010d70881dff3e7958d28a411b476e21bfdd948d`。

### 63. ModRecipesProvider.java

文件：[src/main/java/com/ber/nimblePattern/datagen/ModRecipesProvider.java](../../src/main/java/com/ber/nimblePattern/datagen/ModRecipesProvider.java)。

状态：`=`；1219 字节。

职责：生成样板标记终端无序合成：样板管理终端+逻辑处理器+运算处理器，附解锁条件；当前未生成五种循环元件配方。

改动：与比较基准无差异；保留上述既有实现，纳入整体审核，不虚构本轮修改。

内容指纹：`4eae5850a976155ba8c907241c4648eda4e5b70c0b99dcb0793bd366830b84ae`。

### 64. ModZhCnLangProvider.java

文件：[src/main/java/com/ber/nimblePattern/datagen/ModZhCnLangProvider.java](../../src/main/java/com/ber/nimblePattern/datagen/ModZhCnLangProvider.java)。

状态：`M`；文本差异 +30/-0；5495 字节。

职责：中文对应语言生成源，包括两类失败 toast、工具校验结果和三页签名；与英文及运行翻译键保持一致。

改动：增加与英文键对应的中文概率/工具/循环错误和丢种提醒；相应生成语言文件同改。

内容指纹：`b795d0205f1da999492c2426a1945e0e74a92410299d9b41c97c1a64afbcb7b2`。

### 65. IPatternTagLogicHost.java

文件：[src/main/java/com/ber/nimblePattern/helpers/IPatternTagLogicHost.java](../../src/main/java/com/ber/nimblePattern/helpers/IPatternTagLogicHost.java)。

状态：`=`；267 字节。

职责：Logic 宿主接口，暴露 getLogic/getLevel/markForSave，使库存逻辑不依赖具体部件实现。

改动：与比较基准无差异；保留上述既有实现，纳入整体审核，不虚构本轮修改。

内容指纹：`f968ca549355b8e19f2c8a53e10f01792af4a85fbff7212b719fa278cd071b4f`。

### 66. IPatternTagMenuHost.java

文件：[src/main/java/com/ber/nimblePattern/helpers/IPatternTagMenuHost.java](../../src/main/java/com/ber/nimblePattern/helpers/IPatternTagMenuHost.java)。

状态：`=`；246 字节。

职责：菜单宿主接口，继承 AE IConfigurableObject 并提供 getLogic；供 MenuTypeBuilder 约束宿主类型。

改动：与比较基准无差异；保留上述既有实现，纳入整体审核，不虚构本轮修改。

内容指纹：`bfbc95ed0312e4a50c205926eeace1f88d02015345b3a3e9fa9feccdc810097f`。

### 67. ModCreativeModeTabs.java

文件：[src/main/java/com/ber/nimblePattern/item/ModCreativeModeTabs.java](../../src/main/java/com/ber/nimblePattern/item/ModCreativeModeTabs.java)。

状态：`=`；603 字节。

职责：预留自定义创造栏 DeferredRegister；register 调用被注释，目前实际物品加入 AE 主创造栏。

改动：与比较基准无差异；保留上述既有实现，纳入整体审核，不虚构本轮修改。

内容指纹：`c6f84c0398405c4b0610a7e06182d040f4c83a5830c7c28b0aca35acc5530bf8`。

### 68. ModItems.java

文件：[src/main/java/com/ber/nimblePattern/item/ModItems.java](../../src/main/java/com/ber/nimblePattern/item/ModItems.java)。

状态：`M`；文本差异 +15/-15；2260 字节。

职责：DeferredRegister 注册样板终端 PartItem；遍历 LoopStorageTier 建立不可修改 EnumMap，保留五个 LOOP_STORAGE_CELL_* 常量作为调用兼容入口。

改动：移除五个元件子类构造，改 LoopStorageTier 遍历注册+不可修改 EnumMap；保留原注册常量。

内容指纹：`c954ae116b1261c28e323ecdd06aaeec13cf5dd66a096a9a9541b9ccf6eba805`。

### 69. ILoopStorageCellItem.java

文件：[src/main/java/com/ber/nimblePattern/item/storage/ILoopStorageCellItem.java](../../src/main/java/com/ber/nimblePattern/item/storage/ILoopStorageCellItem.java)。

状态：`M`；文本差异 +5/-0；808 字节。

职责：元件配置 API：容量、配置映射/单键量、累加配置、容量预检查、UUID 和清配置；不提供普通网络自由抽取权限。

改动：补充 canAddConfiguredAmount 预检查和 getOrCreateCellId 身份接口，供循环标记事务使用。

内容指纹：`08c8be3461ab8862b45c219f1d60cfbe9e45361b49d45d547720e6b0fafa35b2`。

### 70. LoopStorageCell16kItem.java

路径：`src/main/java/com/ber/nimblePattern/item/storage/LoopStorageCell16kItem.java`（已删除）。

状态：`D`；文本差异 +0/-7。

职责：仅传容量/耗电参数的旧元件容量派生类。

改动：删除；保留原物品注册 ID，改由 LoopStorageTier + 一个 LoopStorageCellItem 实例类型注册。

### 71. LoopStorageCell1kItem.java

路径：`src/main/java/com/ber/nimblePattern/item/storage/LoopStorageCell1kItem.java`（已删除）。

状态：`D`；文本差异 +0/-7。

职责：仅传容量/耗电参数的旧元件容量派生类。

改动：删除；保留原物品注册 ID，改由 LoopStorageTier + 一个 LoopStorageCellItem 实例类型注册。

### 72. LoopStorageCell256kItem.java

路径：`src/main/java/com/ber/nimblePattern/item/storage/LoopStorageCell256kItem.java`（已删除）。

状态：`D`；文本差异 +0/-7。

职责：仅传容量/耗电参数的旧元件容量派生类。

改动：删除；保留原物品注册 ID，改由 LoopStorageTier + 一个 LoopStorageCellItem 实例类型注册。

### 73. LoopStorageCell4kItem.java

路径：`src/main/java/com/ber/nimblePattern/item/storage/LoopStorageCell4kItem.java`（已删除）。

状态：`D`；文本差异 +0/-7。

职责：仅传容量/耗电参数的旧元件容量派生类。

改动：删除；保留原物品注册 ID，改由 LoopStorageTier + 一个 LoopStorageCellItem 实例类型注册。

### 74. LoopStorageCell64kItem.java

路径：`src/main/java/com/ber/nimblePattern/item/storage/LoopStorageCell64kItem.java`（已删除）。

状态：`D`；文本差异 +0/-7。

职责：仅传容量/耗电参数的旧元件容量派生类。

改动：删除；保留原物品注册 ID，改由 LoopStorageTier + 一个 LoopStorageCellItem 实例类型注册。

### 75. LoopStorageCellAccess.java

文件：[src/main/java/com/ber/nimblePattern/item/storage/LoopStorageCellAccess.java](../../src/main/java/com/ber/nimblePattern/item/storage/LoopStorageCellAccess.java)。

状态：`??`；1699 字节。

职责：访问挂载库存，按 cell UUID+足量种子查指定元件；另提供排除循环元件的可用量汇总给合成快照。

改动：基准中不存在，当前为未跟踪文件；新增内容为上述实现。

内容指纹：`470f7e545ee04d54dc86bca966f29cb5b6ad6486d456c7c094da6333c61a2b27`。

### 76. LoopStorageCellHandler.java

文件：[src/main/java/com/ber/nimblePattern/item/storage/LoopStorageCellHandler.java](../../src/main/java/com/ber/nimblePattern/item/storage/LoopStorageCellHandler.java)。

状态：`=`；880 字节。

职责：AE ICellHandler 单例，以 Item 类型识别本模组元件，创建带原栈/物品/可空 host 的 LoopStorageCellInventory。

改动：与比较基准无差异；保留上述既有实现，纳入整体审核，不虚构本轮修改。

内容指纹：`de1dc4b1bcfaa66260d8fbdfb175abb6befa59b8949e32689888a7ddf4bd5cb7`。

### 77. LoopStorageCellInventory.java

文件：[src/main/java/com/ber/nimblePattern/item/storage/LoopStorageCellInventory.java](../../src/main/java/com/ber/nimblePattern/item/storage/LoopStorageCellInventory.java)。

状态：`M`；文本差异 +54/-21；6214 字节。

职责：StorageCell 混合物品/流体实现；IO 源且 host=null 才准普通读写；循环专用取/还；按标记和容量收货、真实字节灯色、即时 persist 与 host.saveChanges。

改动：追加 UUID/循环专用取还和即时持久化；IO 端口取出权限与 host=null 限制；灯色按真实用量而非标记是否满额。

内容指纹：`aeaed4001a9da8e3582281679800aaadbe33da5493e147d0aa94b10191266163`。

### 78. LoopStorageCellItem.java

文件：[src/main/java/com/ber/nimblePattern/item/storage/LoopStorageCellItem.java](../../src/main/java/com/ber/nimblePattern/item/storage/LoopStorageCellItem.java)。

状态：`M`；文本差异 +91/-4；8608 字节。

职责：单一物品实现：枚举容量/耗电，配置累加/容量检查/UUID、contents NBT 编解码、饱和算术、字节/类别 tooltip 和 AE 存储内容图片。

改动：构造改为 LoopStorageTier；新增容量预检查和元件 UUID；新增 AE 字节/类别和内容 tooltip；提取共享 bytesUsed；原配置累加与 NBT 名保留。

内容指纹：`f0fee761d80f5639d2ef7a8c8c2b15899aee50e3335ef88721c4f68566c30bd2`。

### 79. LoopStorageCellVisibility.java

文件：[src/main/java/com/ber/nimblePattern/item/storage/LoopStorageCellVisibility.java](../../src/main/java/com/ber/nimblePattern/item/storage/LoopStorageCellVisibility.java)。

状态：`M`；文本差异 +1/-1；1257 字节。

职责：通过 DelegatingMEInventory accessor 解开包装，用 identity visited 防循环；区分循环元件与其他 StorageCell，供列表隐藏和种子搜索。

改动：unwrapCell 从内部辅助提升可复用访问，供 LoopStorageCellAccess 定位具体盘。

内容指纹：`ff4fe3b59b79281cc942cc08b51ff66ca7eeb42a3867bacc407a364673badc7d`。

### 80. LoopStorageTier.java

文件：[src/main/java/com/ber/nimblePattern/item/storage/LoopStorageTier.java](../../src/main/java/com/ber/nimblePattern/item/storage/LoopStorageTier.java)。

状态：`??`；671 字节。

职责：五档容量数据枚举，委托 AE StorageTier.bytes/idleDrain/namePrefix，生成稳定 loop_storage_cell_* 注册名；替代五个空子类。

改动：基准中不存在，当前为未跟踪文件；新增内容为上述实现。

内容指纹：`d67f1bb04b80b0336d570ff76f14399d45b78afb24abc5a2f14e3222b7ed013a`。

### 81. PatternTagTermMenu.java

文件：[src/main/java/com/ber/nimblePattern/menu/PatternTagTermMenu.java](../../src/main/java/com/ber/nimblePattern/menu/PatternTagTermMenu.java)。

状态：`M`；文本差异 +183/-3；33376 字节。

职责：服务端终端控制中心：槽语义、模式同步/持久化、三种应用校验、来源回填、远程取样板、容器发现、全量/增量同步、历史条件计数和五 tick 节流。

改动：新增循环/工具 FakeSlot 与受限元件槽、applyLoop/applyTools 校验与可选回填；mode 与 Logic 同步保存；网络扫描五 tick 节流，条件 dirty 和库存 revision 缓存。原更新应用/远程浏览逻辑继续复用。

内容指纹：`b3b5628a751f934939f3c7c289b8882366a02db7884b8476b35a530d530fd236`。

### 82. LoopStorageCellSlot.java

文件：[src/main/java/com/ber/nimblePattern/menu/slot/LoopStorageCellSlot.java](../../src/main/java/com/ber/nimblePattern/menu/slot/LoopStorageCellSlot.java)。

状态：`??`；638 字节。

职责：RestrictedInputSlot(STORAGE_CELLS) 子类，叠加 LoopStorageCellItem 类型限制及每槽一个，防止放入普通元件。

改动：基准中不存在，当前为未跟踪文件；新增内容为上述实现。

内容指纹：`8e04260542911fe214af6c0737e2da8a5414cd91392c8ca3d0004d5278b3ab35`。

### 83. CraftingCalculationAccessorMixin.java

文件：[src/main/java/com/ber/nimblePattern/mixin/ae2/CraftingCalculationAccessorMixin.java](../../src/main/java/com/ber/nimblePattern/mixin/ae2/CraftingCalculationAccessorMixin.java)。

状态：`??`；748 字节。

职责：访问 AE CraftingCalculation.networkInv/simRequester，调用 addMissing；工具使用快照，循环使用请求者网格作种子检查。

改动：基准中不存在，当前为未跟踪文件；新增内容为上述实现。

内容指纹：`a97da9064dd6f60e71ad38be2d0de428998387570f5ddcf6496f5b50b3465d2f`。

### 84. CraftingCpuLogicMixin.java

文件：[src/main/java/com/ber/nimblePattern/mixin/ae2/CraftingCpuLogicMixin.java](../../src/main/java/com/ber/nimblePattern/mixin/ae2/CraftingCpuLogicMixin.java)。

状态：`M`；文本差异 +260/-54；19857 字节。

职责：执行中枢注入：工具预留/归还、循环启动与活跃接管、native 投料、概率派发计数、特殊收货顺序、模糊账本、假完成、最终交付和取消/存档钩子。

改动：从处理样板专用引用转统一包装；新增工具/循环执行与取消、概率派发/补料协调、统一特殊收货；模糊 Map 改 FuzzyOutputLedger；CPU NBT 记工具/模糊；修复最终实体产物与欠账重复结算。

内容指纹：`f89c56a9b9bb6db9165766332782d9a47298bc410e9caf9e8490631c47794e76`。

### 85. CraftingTreeProcessMixin.java

文件：[src/main/java/com/ber/nimblePattern/mixin/ae2/CraftingTreeProcessMixin.java](../../src/main/java/com/ber/nimblePattern/mixin/ae2/CraftingTreeProcessMixin.java)。

状态：`??`；2348 字节。

职责：在请求循环 composite 前检查指定元件种子；正常缺料抛 CraftBranchFailure，模拟缺料只 addMissing 一次，不把种子列成递归耗材。

改动：基准中不存在，当前为未跟踪文件；新增内容为上述实现。

内容指纹：`50c6322caa19a2be32e1b1517bccb3ee63a20bcb28ee74749c2c07f161c4d6e3`。

### 86. DelegatingMEInventoryMixin.java

文件：[src/main/java/com/ber/nimblePattern/mixin/ae2/DelegatingMEInventoryMixin.java](../../src/main/java/com/ber/nimblePattern/mixin/ae2/DelegatingMEInventoryMixin.java)。

状态：`=`；395 字节。

职责：只读 delegate accessor，支持识别被 AE 包装的真实 StorageCell；不修改存储行为。

改动：与比较基准无差异；保留上述既有实现，纳入整体审核，不虚构本轮修改。

内容指纹：`a764fbb4110ebd32d85d24642e1d6f3b471c63e39e160b89d3c719cee6c7cbb2`。

### 87. EncodedPatternItemMixin.java

文件：[src/main/java/com/ber/nimblePattern/mixin/ae2/EncodedPatternItemMixin.java](../../src/main/java/com/ber/nimblePattern/mixin/ae2/EncodedPatternItemMixin.java)。

状态：`M`；文本差异 +12/-0；2944 字节。

职责：编码样板 tooltip 添加概率标记、工具列表和更新条件/状态；对条件注册 ID 解析本地化名称；没有循环专用标签展示逻辑。

改动：在原更新条件 tooltip 前新增概率标记和工具列表。

内容指纹：`cb9f62dd73c975d84723ee71ac4ee1e69efe2427a0877ce3d5fd178873c60fe9`。

### 88. ExecutingCraftingJobAccessorMixin.java

文件：[src/main/java/com/ber/nimblePattern/mixin/ae2/ExecutingCraftingJobAccessorMixin.java](../../src/main/java/com/ber/nimblePattern/mixin/ae2/ExecutingCraftingJobAccessorMixin.java)。

状态：`??`；501 字节。

职责：读 remainingAmount 限制本次最终结账；读可空 playerId 定位循环取消通知玩家。

改动：基准中不存在，当前为未跟踪文件；新增内容为上述实现。

内容指纹：`44d05f205e84dce02ce69683427693f556efd2f239debaa6ad40a7d4326100cd`。

### 89. GtlLoopBatchMixin.java

文件：[src/main/java/com/ber/nimblePattern/mixin/ae2/GtlLoopBatchMixin.java](../../src/main/java/com/ber/nimblePattern/mixin/ae2/GtlLoopBatchMixin.java)。

状态：`??`；2833 字节。

职责：可选 getOperations 注入：工具限制一轮，native 身份查询批数上限；概率按 cycles/output long 上限约束；捕获循环/概率批次数。

改动：基准中不存在，当前为未跟踪文件；新增内容为上述实现。

内容指纹：`271dd4357ac5502dd4b932d0636ae195e615e6bba67c2f16fcfcda3ee5464048`。

### 90. NetworkCraftingProviderStateMixin.java

文件：[src/main/java/com/ber/nimblePattern/mixin/ae2/NetworkCraftingProviderStateMixin.java](../../src/main/java/com/ber/nimblePattern/mixin/ae2/NetworkCraftingProviderStateMixin.java)。

状态：`??`；5226 字节。

职责：所有供应器进入 AE 时统一编译；从真实库存恢复循环/概率标签，登记 native 映射并在 unmount 用令牌释放。

改动：基准中不存在，当前为未跟踪文件；新增内容为上述实现。

内容指纹：`f2bb683564e56abd003e07f45c5a90ce00dd71ea53ed4ec1b5eebe3c3e9481ca`。

### 91. NetworkCraftingSimulationStateMixin.java

文件：[src/main/java/com/ber/nimblePattern/mixin/ae2/NetworkCraftingSimulationStateMixin.java](../../src/main/java/com/ber/nimblePattern/mixin/ae2/NetworkCraftingSimulationStateMixin.java)。

状态：`??`；1754 字节。

职责：网络规划库存构造后排除循环元件；可按 AE 配置模拟抽取其他库存，避免可见种子被当普通材料。

改动：基准中不存在，当前为未跟踪文件；新增内容为上述实现。

内容指纹：`2405aa2076e6b74103c835269bb80af0da5f1bbc61d4728f5a81448df281db7a`。

### 92. NetworkCraftingSnapshotAccessor.java

文件：[src/main/java/com/ber/nimblePattern/mixin/ae2/NetworkCraftingSnapshotAccessor.java](../../src/main/java/com/ber/nimblePattern/mixin/ae2/NetworkCraftingSnapshotAccessor.java)。

状态：`??`；417 字节。

职责：只读 AE 网络模拟库存 list，用于异步工具预留计划的安全快照访问。

改动：基准中不存在，当前为未跟踪文件；新增内容为上述实现。

内容指纹：`afcd19be836df1ec7180116b14291dcb3227acc798c4ddcd96f6af8ce3cb745e`。

### 93. NetworkStorageAccessorMixin.java

文件：[src/main/java/com/ber/nimblePattern/mixin/ae2/NetworkStorageAccessorMixin.java](../../src/main/java/com/ber/nimblePattern/mixin/ae2/NetworkStorageAccessorMixin.java)。

状态：`??`；484 字节。

职责：暴露 NetworkStorage.priorityInventory；循环元件查找和可用量排除依赖该 AE 私有字段。

改动：基准中不存在，当前为未跟踪文件；新增内容为上述实现。

内容指纹：`1c34586c77557f016212995206492ddab0f1ba1a18b19969ffa4abe885cf6b5c`。

### 94. NetworkStorageMixin.java

文件：[src/main/java/com/ber/nimblePattern/mixin/ae2/NetworkStorageMixin.java](../../src/main/java/com/ber/nimblePattern/mixin/ae2/NetworkStorageMixin.java)。

状态：`M`；文本差异 +2/-3；3156 字节。

职责：真实入网成功后提交更新条件 ID；同时存在普通/循环元件时重建不含循环内容的列表，保留 mountsInUse 重入防护。

改动：更新事件由字符串 ID 转 ResourceLocation，并不再因当前追踪集空而跳过；元件可见性逻辑保留。

内容指纹：`7fe0a9ec6b2fcd30d72f5f7a7c64a70888c7e4b925f476f20b543b6a4357e245`。

### 95. PatternProviderBlockEntityMixin.java

文件：[src/main/java/com/ber/nimblePattern/mixin/ae2/PatternProviderBlockEntityMixin.java](../../src/main/java/com/ber/nimblePattern/mixin/ae2/PatternProviderBlockEntityMixin.java)。

状态：`=`；857 字节。

职责：方块供应器实现 IUpgradeableObject，把 getUpgrades 委托到逻辑，支持 Shift 右键装卡。

改动：与比较基准无差异；保留上述既有实现，纳入整体审核，不虚构本轮修改。

内容指纹：`4fd9c4d0c2d211dc4b0a921f20f63042377f56288a21cc295e54203652f9a651`。

### 96. PatternProviderLogicMixin.java

文件：[src/main/java/com/ber/nimblePattern/mixin/ae2/PatternProviderLogicMixin.java](../../src/main/java/com/ber/nimblePattern/mixin/ae2/PatternProviderLogicMixin.java)。

状态：`M`；文本差异 +17/-6；5825 字节。

职责：为供应器建立一格模糊卡升级库、持久化/掉落/清空；编译包装及缓存；适配 contains 和按 dropSecondary 的回货解锁。

改动：包装类升级为 NimbleEncodedPattern.wrap，保留装配室能力；新增 raw 对象序列/模糊状态编译缓存，调整相关解包类型。

内容指纹：`bd5541eeaa7ebf8c467f552ecf32671d2663ec1c7bf5b7ff97f7436c9174bbe4`。

### 97. PatternProviderMenuMixin.java

文件：[src/main/java/com/ber/nimblePattern/mixin/ae2/PatternProviderMenuMixin.java](../../src/main/java/com/ber/nimblePattern/mixin/ae2/PatternProviderMenuMixin.java)。

状态：`=`；2076 字节。

职责：供应器菜单加入 UPGRADE 受限槽，前置以控制 shift-click 优先级，重新编号 slots.index。

改动：与比较基准无差异；保留上述既有实现，纳入整体审核，不虚构本轮修改。

内容指纹：`eb9dd7f351f8000cb80fd8d55374ccac054d461d4342c97ccd07496abd505596`。

### 98. PatternProviderPartMixin.java

文件：[src/main/java/com/ber/nimblePattern/mixin/ae2/PatternProviderPartMixin.java](../../src/main/java/com/ber/nimblePattern/mixin/ae2/PatternProviderPartMixin.java)。

状态：`=`；829 字节。

职责：部件供应器实现 IUpgradeableObject，委托逻辑升级库；与方块版本保持卡片交互一致。

改动：与比较基准无差异；保留上述既有实现，纳入整体审核，不虚构本轮修改。

内容指纹：`0cf99dc2850219f7a9904fe550397ef137225432bdb07dd489c087ab2707d19f`。

### 99. PatternProviderScreenMixin.java

文件：[src/main/java/com/ber/nimblePattern/mixin/ae2/PatternProviderScreenMixin.java](../../src/main/java/com/ber/nimblePattern/mixin/ae2/PatternProviderScreenMixin.java)。

状态：`=`；1813 字节。

职责：客户端供应器 screen 构造时挂 UpgradesPanel，并显示兼容模糊卡列表。

改动：与比较基准无差异；保留上述既有实现，纳入整体审核，不虚构本轮修改。

内容指纹：`bf7a363568b8880de989379ce23e6931e438e9aa17ed7e27c6fbe43e3d9b4827`。

### 100. ProbabilityCpuMixin.java

文件：[src/main/java/com/ber/nimblePattern/mixin/ae2/ProbabilityCpuMixin.java](../../src/main/java/com/ber/nimblePattern/mixin/ae2/ProbabilityCpuMixin.java)。

状态：`??`；2779 字节。

职责：CPU 概率能力/懒控制器；tick 处理、补料等待展示、finish 清理、nimble_probability 存档与 ACTIVE 恢复。

改动：基准中不存在，当前为未跟踪文件；新增内容为上述实现。

内容指纹：`88edc402974b9a3f81985eca84133cbd09cd3cf8ae2363276faab58b15efa0fb`。

### 101. ProbabilityEncodingMenuMixin.java

文件：[src/main/java/com/ber/nimblePattern/mixin/ae2/ProbabilityEncodingMenuMixin.java](../../src/main/java/com/ber/nimblePattern/mixin/ae2/ProbabilityEncodingMenuMixin.java)。

状态：`??`；3379 字节。

职责：服务端按 recipeId 做初次归一化并保留来源快照；编码检查现有概率输出，允许后续编辑；相同内容再次编码仅取消标记。

改动：基准中不存在，当前为未跟踪文件；新增内容为上述实现。

内容指纹：`1a9f48c435e428ccf486e57e7e554437c93e0475ebf9586756a13a81758eb435`。

### 102. ProbabilityGtCompletionMixin.java

文件：[src/main/java/com/ber/nimblePattern/mixin/ae2/ProbabilityGtCompletionMixin.java](../../src/main/java/com/ber/nimblePattern/mixin/ae2/ProbabilityGtCompletionMixin.java)。

状态：`??`；1666 字节。

职责：可选 GT RecipeLogic 完成 HEAD/TAIL；仅有票据时取 origin recipeId 和 actual.parallels，向 ProbabilityMachines 分摊完成次数。

改动：基准中不存在，当前为未跟踪文件；新增内容为上述实现。

内容指纹：`f0cfe99bde6a279e134213e84c8fbb7d682fd5aee2dbe90bc58d04073c8acc2f`。

### 103. ProbabilityJeiTransferMixin.java

文件：[src/main/java/com/ber/nimblePattern/mixin/ae2/ProbabilityJeiTransferMixin.java](../../src/main/java/com/ber/nimblePattern/mixin/ae2/ProbabilityJeiTransferMixin.java)。

状态：`??`；1525 字节。

职责：客户端可选 JEI transfer 成功后发当前 containerId/配方 ID，未知配方发哨兵使旧概率来源失效。

改动：基准中不存在，当前为未跟踪文件；新增内容为上述实现。

内容指纹：`ab05841cc90e44ca7fbffca55ea92acafaa2ea4ed53bcb2ec4436c4fb41ef413`。

### 104. ProbabilityJobMixin.java

文件：[src/main/java/com/ber/nimblePattern/mixin/ae2/ProbabilityJobMixin.java](../../src/main/java/com/ber/nimblePattern/mixin/ae2/ProbabilityJobMixin.java)。

状态：`??`；2742 字节。

职责：实现 ProbabilityJob，访问原 job waitingFor，将补单 patternTimes 合并 tasks；反射 TaskProgress.value 和时间统计方法，替代产物不重复计工作。

改动：基准中不存在，当前为未跟踪文件；新增内容为上述实现。

内容指纹：`1dfe47e379997775e1e2564da4046f1a2c93fac793391e3b982b1cc7d87ff967`。

### 105. ProbabilityPriorityMixin.java

文件：[src/main/java/com/ber/nimblePattern/mixin/ae2/ProbabilityPriorityMixin.java](../../src/main/java/com/ber/nimblePattern/mixin/ae2/ProbabilityPriorityMixin.java)。

状态：`??`；1362 字节。

职责：CraftingService.insertIntoCpus HEAD 优先给活跃概率 CPU；剩余给非概率 CPU，无活动则保留原 AE 路径。

改动：基准中不存在，当前为未跟踪文件；新增内容为上述实现。

内容指纹：`d4fa283b49c40d426bffe5265061c388e85ec4f46226b894e1ff595b698f94ca`。

### 106. ProbabilityProviderTargetMixin.java

文件：[src/main/java/com/ber/nimblePattern/mixin/ae2/ProbabilityProviderTargetMixin.java](../../src/main/java/com/ber/nimblePattern/mixin/ae2/ProbabilityProviderTargetMixin.java)。

状态：`??`；1955 字节。

职责：记录标准 AE 供应器一次 push 的投料目标，支持邻接机器解析；每次先清旧目标，防沿用上次地址。

改动：基准中不存在，当前为未跟踪文件；新增内容为上述实现。

内容指纹：`1a96050d52a85d5cb4a4d0fccf4fd3c60e9f1cbf49791bcf7be27bca6f0619d4`。

### 107. ToolCraftingPlanMixin.java

文件：[src/main/java/com/ber/nimblePattern/mixin/ae2/ToolCraftingPlanMixin.java](../../src/main/java/com/ber/nimblePattern/mixin/ae2/ToolCraftingPlanMixin.java)。

状态：`??`；1196 字节。

职责：buildCraftingPlan RETURN 调用 ToolCraftingPlan.reserve，显式从合成计算 networkInv 的快照读取可用工具量。

改动：基准中不存在，当前为未跟踪文件；新增内容为上述实现。

内容指纹：`6b17c9cf19366256a71a309bf9594e49a4e005dabd67a5dfce11399a79a09dbd`。

### 108. ToolJobReloadMixin.java

文件：[src/main/java/com/ber/nimblePattern/mixin/ae2/ToolJobReloadMixin.java](../../src/main/java/com/ber/nimblePattern/mixin/ae2/ToolJobReloadMixin.java)。

状态：`??`；1617 字节。

职责：NBT 任务构造时重定向 decodePattern，剥离 execution_fuzzy 后解码再 wrap；不只恢复工具，也恢复其他统一包装能力。

改动：基准中不存在，当前为未跟踪文件；新增内容为上述实现。

内容指纹：`eb27094b7c0787185262378f23702db1ca3d29b356fdf19bf636f22da9b309f1`。

### 109. PartExPatternProviderMixin.java

文件：[src/main/java/com/ber/nimblePattern/mixin/extendedae/PartExPatternProviderMixin.java](../../src/main/java/com/ber/nimblePattern/mixin/extendedae/PartExPatternProviderMixin.java)。

状态：`=`；939 字节。

职责：可选 ExtendedAE 部件供应器的 IUpgradeableObject 接口桥接；直接引用可选类，由独立非 required 配置管理。

改动：与比较基准无差异；保留上述既有实现，纳入整体审核，不虚构本轮修改。

内容指纹：`e68621269442983f7bcbe57659f166a290bdff3e51918720980b632e2ba94efd`。

### 110. ModDataGenerator.java

文件：[src/main/java/com/ber/nimblePattern/ModDataGenerator.java](../../src/main/java/com/ber/nimblePattern/ModDataGenerator.java)。

状态：`=`；1667 字节。

职责：GatherDataEvent 根据 includeServer/includeClient 注册配方、方块/物品标签、物品模型和两种语言 provider；src/generated/resources 是构建资源输入。

改动：与比较基准无差异；保留上述既有实现，纳入整体审核，不虚构本轮修改。

内容指纹：`5b48e76539b82496faf48fdf6975365315b59d650a753f972a9a2e83593d7226`。

### 111. ClearPacket.java

文件：[src/main/java/com/ber/nimblePattern/network/ClearPacket.java](../../src/main/java/com/ber/nimblePattern/network/ClearPacket.java)。

状态：`=`；829 字节。

职责：全量浏览同步开始的空包，客户端清空记录并延迟重建；未携带 containerId。

改动：与比较基准无差异；保留上述既有实现，纳入整体审核，不虚构本轮修改。

内容指纹：`b38615b1bf56147a65619567617c4f7bc03fcef921a7086dcb0649a8856ac946`。

### 112. ConditionPacket.java

文件：[src/main/java/com/ber/nimblePattern/network/ConditionPacket.java](../../src/main/java/com/ber/nimblePattern/network/ConditionPacket.java)。

状态：`=`；1451 字节。

职责：传输排序后的条件字符串集合，更新输入框历史补全；decode 按载荷计数创建集合。

改动：与比较基准无差异；保留上述既有实现，纳入整体审核，不虚构本轮修改。

内容指纹：`9947fe3788e3e0cea966ae922449e3ee295b8ed495296670bac952c4a73372f5`。

### 113. LoopSeedLostNotificationPacket.java

文件：[src/main/java/com/ber/nimblePattern/network/LoopSeedLostNotificationPacket.java](../../src/main/java/com/ber/nimblePattern/network/LoopSeedLostNotificationPacket.java)。

状态：`??`；1282 字节。

职责：AEKey 编码/解码，客户端隔离执行失种提示；CPU 在可找到发起玩家时发送。

改动：基准中不存在，当前为未跟踪文件；新增内容为上述实现。

内容指纹：`8fe5b389250f6268f512a6d3d7054f1618043b8fdca2e518b9e4d5822e7e195d`。

### 114. NimblePatternNetwork.java

文件：[src/main/java/com/ber/nimblePattern/network/NimblePatternNetwork.java](../../src/main/java/com/ber/nimblePattern/network/NimblePatternNetwork.java)。

状态：`M`；文本差异 +6/-1；2159 字节。

职责：建立协议版本 2 的 SimpleChannel，注册 ID 0..7；概率 recipe/failure 包分别显式限定 C2S/S2C。

改动：协议 1→2；新增 seed lost、probability recipe、probability failure 包注册，概率两包显式限制方向。

内容指纹：`9186d75fbb4bac394750de24f29a33f41345ec88ff103ecd54da0b43cbb0c8c5`。

### 115. PatternPacket.java

文件：[src/main/java/com/ber/nimblePattern/network/PatternPacket.java](../../src/main/java/com/ber/nimblePattern/network/PatternPacket.java)。

状态：`=`；2926 字节。

职责：远程容器全量/增量包；包含 serverId、full、可选 size 与槽位 ItemStack Map；交当前终端 screen 更新镜像。

改动：与比较基准无差异；保留上述既有实现，纳入整体审核，不虚构本轮修改。

内容指纹：`df813b39d836dfe37248d1194cb4dbc37a8e6e17d3e7c57e64e6f6ba3de602dc`。

### 116. PatternSyncCompletePacket.java

文件：[src/main/java/com/ber/nimblePattern/network/PatternSyncCompletePacket.java](../../src/main/java/com/ber/nimblePattern/network/PatternSyncCompletePacket.java)。

状态：`=`；994 字节。

职责：空的全量结束标记；触发客户端一次 rebuildPatternView，不再逐容器排序。

改动：与比较基准无差异；保留上述既有实现，纳入整体审核，不虚构本轮修改。

内容指纹：`a8f9b347e405a5c7abf4e4149625b93e41f1e6d8f1420c829802c76342b78016`。

### 117. PatternUpgradeNotificationPacket.java

文件：[src/main/java/com/ber/nimblePattern/network/PatternUpgradeNotificationPacket.java](../../src/main/java/com/ber/nimblePattern/network/PatternUpgradeNotificationPacket.java)。

状态：`M`；文本差异 +8/-3；2910 字节。

职责：condition/count 通知，DistExecutor 客户端隔离；当前正开终端不弹 toast，其他情况解析物品/流体名称图标。

改动：handle 使用 DistExecutor 与内部 ClientHandler 隔离客户端展示，原提示行为保留。

内容指纹：`9a68913165c0ff1fd7696dea80e31277a49d6c0cd6af35cdd87da92f3fc00bce`。

### 118. ProbabilityFailurePacket.java

文件：[src/main/java/com/ber/nimblePattern/network/ProbabilityFailurePacket.java](../../src/main/java/com/ber/nimblePattern/network/ProbabilityFailurePacket.java)。

状态：`??`；1313 字节。

职责：广播概率缺料产物 AEKey；用 LoopSeedLostToast 的可配置翻译键绘制概率失败提示。

改动：基准中不存在，当前为未跟踪文件；新增内容为上述实现。

内容指纹：`abdf2d226bf0765c6d44db70da697c008988effc010f7944f79c6e09e2adee9a`。

### 119. ProbabilityRecipePacket.java

文件：[src/main/java/com/ber/nimblePattern/network/ProbabilityRecipePacket.java](../../src/main/java/com/ber/nimblePattern/network/ProbabilityRecipePacket.java)。

状态：`??`；1293 字节。

职责：C2S 只传 containerId/recipeId；验证当前服务端菜单再查询自己的 RecipeManager，不接受客户端直接上传成功率。

改动：基准中不存在，当前为未跟踪文件；新增内容为上述实现。

内容指纹：`2a0455999b77bbdd50eec13e71def0480a8f4421568f537930b5345308b21690`。

### 120. NimblePattern.java

文件：[src/main/java/com/ber/nimblePattern/NimblePattern.java](../../src/main/java/com/ber/nimblePattern/NimblePattern.java)。

状态：`M`；文本差异 +9/-1；4214 字节。

职责：模组生命周期入口：注册物品、菜单、网络通道、部件模型、COMMON 配置、元件 handler 和供应器模糊卡；END tick 调更新追踪；停服清理更新/供应器/概率静态状态。

改动：新增 ServerStoppedEvent 清理 ProviderPatternIndex、ProbabilityController ACTIVE、ProbabilityMachines 及更新追踪；已有注册和 tick 逻辑保留。Git 索引路径的文件名为 Nimblepattern.java，注意大小写。

内容指纹：`8e4c09ff7d731979556e5e0ef0df6805b20b605c0c1f2aab9d25a38c3ffdf70b`。

### 121. PatternTagLogic.java

文件：[src/main/java/com/ber/nimblePattern/parts/PatternTagLogic.java](../../src/main/java/com/ber/nimblePattern/parts/PatternTagLogic.java)。

状态：`M`；文本差异 +41/-0；3696 字节。

职责：宿主持久状态：81 样板、1 条件、2 端点、1 元件、4 工具和 TagMode；库存变化 markForSave，载入期间 suppress 重复保存。

改动：新增循环端点、元件、工具库存和 mode 状态；NBT 新增相应字段与非法 mode 回退。

内容指纹：`70865edbbe056660ccb27d60e14c1884fa3eb3244f26e5adaa749cf28627e5e0`。

### 122. PatternTagTerminalPart.java

文件：[src/main/java/com/ber/nimblePattern/parts/PatternTagTerminalPart.java](../../src/main/java/com/ber/nimblePattern/parts/PatternTagTerminalPart.java)。

状态：`M`；文本差异 +6/-0；3912 字节。

职责：AE 显示部件开菜单、静态模型、配置、Logic NBT；拆除掉落实体样板/元件，清理虚拟端点和工具。

改动：新增元件真实掉落，clearContent 清端点/工具/元件；原部件激活和保存框架复用。

内容指纹：`73542b2149496b3be54b20308285e1d0a5108a253fc6b7f9cbf37c79f3892230`。

### 123. TagMode.java

文件：[src/main/java/com/ber/nimblePattern/parts/TagMode.java](../../src/main/java/com/ber/nimblePattern/parts/TagMode.java)。

状态：`M`；文本差异 +2/-1；99 字节。

职责：UPGRADE/LOOP/TOOL 枚举；顺序对应 modePanel0..2，name 写 NBT。改顺序/名字需同时审布局/存档。

改动：在原 UPGRADE/LOOP 基础上追加 TOOL，不重排前两种模式。

内容指纹：`f24dc3152f9a9c85ea95a519d0392e7ac7f80aae684ef9896b6123d3b32583f2`。

### 124. LoopPatternData.java

文件：[src/main/java/com/ber/nimblePattern/pattern/LoopPatternData.java](../../src/main/java/com/ber/nimblePattern/pattern/LoopPatternData.java)。

状态：`??`；661 字节。

职责：不可变环数据 record，含组/元件 UUID、序号/数量、入口/出口、种子/净量、外部输入/各主产物；列表防外部修改，isClosingPattern 判末步。

改动：基准中不存在，当前为未跟踪文件；新增内容为上述实现。

内容指纹：`f3507ee8548b460bd98a313af30d32330f624ff84579c350888a39661e1a4f2a`。

### 125. LoopPatternParser.java

文件：[src/main/java/com/ber/nimblePattern/pattern/LoopPatternParser.java](../../src/main/java/com/ber/nimblePattern/pattern/LoopPatternParser.java)。

状态：`??`；6443 字节。

职责：>=2 处理样板线性唯一找环、端点/平衡/闭合/净增校验；累加外部输入，保留 sourceIndices；为全环各样板生成 metadata。

改动：基准中不存在，当前为未跟踪文件；新增内容为上述实现。

内容指纹：`36300340b582a191ca776d6f3b367ec1f7d1a93a291f85676b31d0e876bbc49e`。

### 126. NimbleAssemblerPattern.java

文件：[src/main/java/com/ber/nimblePattern/pattern/NimbleAssemblerPattern.java](../../src/main/java/com/ber/nimblePattern/pattern/NimbleAssemblerPattern.java)。

状态：`??`；3709 字节。

职责：保留 IMolecularAssemblerSupportedPattern 接口委托，重验工具；向计划器隐藏工具消耗、减保留量得到净产出，向供应器仍派原始配方。

改动：基准中不存在，当前为未跟踪文件；新增内容为上述实现。

内容指纹：`f22045c14a334a2c00b97738d480ed961fd5e3c0212001c50735434db2906216`。

### 127. NimbleEncodedPattern.java

文件：[src/main/java/com/ber/nimblePattern/pattern/NimbleEncodedPattern.java](../../src/main/java/com/ber/nimblePattern/pattern/NimbleEncodedPattern.java)。

状态：`??`；7455 字节。

职责：IPatternDetails 通用包装；缓存 metadata/definition/去管理标签的 identity/hash；模糊输入、假样板判定、循环末步折叠；工厂保留装配室能力。

改动：基准中不存在，当前为未跟踪文件；新增内容为上述实现。

内容指纹：`6a3dec22cc698a274e84b7c14bca82a4d318b5d279a5d70cd5ff622bc4b19694`。

### 128. NimblePatternSource.java

文件：[src/main/java/com/ber/nimblePattern/pattern/NimblePatternSource.java](../../src/main/java/com/ber/nimblePattern/pattern/NimblePatternSource.java)。

状态：`=`；380 字节。

职责：来源定位记录：ServerLevel、BlockPos、Direction、slot、InternalInventory；临时解析结果而非独立持久对象。

改动：与比较基准无差异；保留上述既有实现，纳入整体审核，不虚构本轮修改。

内容指纹：`653d3ec99195f853fb312a0441f3a6a92781443fd0f07b4501cc67da033cf0bb`。

### 129. NimblePatternTag.java

文件：[src/main/java/com/ber/nimblePattern/pattern/NimblePatternTag.java](../../src/main/java/com/ber/nimblePattern/pattern/NimblePatternTag.java)。

状态：`M`；文本差异 +79/-0；13801 字节。

职责：来源/更新/循环 NBT API，回填原槽（含可选 ExtendedAE、GT/LDlib 反射库存适配）；tagLoop 清概率/工具标签；loop 部分字段解码保护。

改动：新增 loop NBT 写/读/清及 GenericStack 列表序列化；写 loop 时清 probability/tools；来源及更新标签逻辑是基线既有实现。

内容指纹：`fe0d5f626ee8d9cc88ca308ac7efc8baaf9e16bce47c9cbe02d6640816746923`。

### 130. NimbleProcessingPattern.java

路径：`src/main/java/com/ber/nimblePattern/pattern/NimbleProcessingPattern.java`（已删除）。

状态：`D`；文本差异 +0/-113。

职责：旧处理样板专用包装类，曾承载模糊/假样板语义。

改动：删除；由 NimbleEncodedPattern 统一身份/元数据与 NimbleAssemblerPattern 保留装配室能力替代。旧类名不应再出现在运行引用。

### 131. PatternInventorySnapshots.java

文件：[src/main/java/com/ber/nimblePattern/pattern/PatternInventorySnapshots.java](../../src/main/java/com/ber/nimblePattern/pattern/PatternInventorySnapshots.java)。

状态：`??`；2093 字节。

职责：服务端 WeakHashMap 共享库存深拷贝快照，默认五 tick 节流、force 绕过；只复制变化槽并递增 revision，供终端和条件索引使用。

改动：基准中不存在，当前为未跟踪文件；新增内容为上述实现。

内容指纹：`d00c353b1c5cd513b912d54f9eddb796cd91e6b509dbb44d4e6a0a6f833d0de8`。

### 132. PatternMetadata.java

文件：[src/main/java/com/ber/nimblePattern/pattern/PatternMetadata.java](../../src/main/java/com/ber/nimblePattern/pattern/PatternMetadata.java)。

状态：`??`；810 字节。

职责：统一只读解码结果 record：概率标志/recipe/cycles、工具列表、环数据；把 NBT 读取集中在包装构建阶段。

改动：基准中不存在，当前为未跟踪文件；新增内容为上述实现。

内容指纹：`60cc2f3a307cadb9e7a0f12215a2fbc9a5fa9bcf88416fdfd33284734bc21e14`。

### 133. PatternUpgradeTracker.java

文件：[src/main/java/com/ber/nimblePattern/pattern/PatternUpgradeTracker.java](../../src/main/java/com/ber/nimblePattern/pattern/PatternUpgradeTracker.java)。

状态：`M`；文本差异 +75/-26；7885 字节。

职责：全服活动网络条件追踪；20 tick 去重轮询，按库存 revision 更新反向索引，真实入网 ID 去重后标状态，给满足无线终端条件的玩家提醒。

改动：移除对打开终端提供条件集合的依赖；改全服活动网格发现、20 tick 队列、ResourceLocation 去重、库存版本索引增量维护与统一 clear。

内容指纹：`692683fc80ff2e788e9a9ed21708a8959315fa3c191fb33e00315c61d69efb47`。

### 134. ProviderPatternIndex.java

文件：[src/main/java/com/ber/nimblePattern/pattern/ProviderPatternIndex.java](../../src/main/java/com/ber/nimblePattern/pattern/ProviderPatternIndex.java)。

状态：`??`；1623 字节。

职责：弱键供应器→compiled/native 映射；重建返回令牌，旧 unmount 不得删新映射；CPU 与 GTL 批数查询使用 nativePattern。

改动：基准中不存在，当前为未跟踪文件；新增内容为上述实现。

内容指纹：`73d3cbab947048a8a447ddc83103d8575c7b6e2e937cca28d7f5cf802301fa15`。

### 135. ToolPatternData.java

文件：[src/main/java/com/ber/nimblePattern/pattern/ToolPatternData.java](../../src/main/java/com/ber/nimblePattern/pattern/ToolPatternData.java)。

状态：`??`；2983 字节。

职责：tools NBT 读写和逐配方工具合法性：仅 AECraftingPattern，首选输入、Unbreakable+原样余物或正净增产；写入去循环标签。

改动：基准中不存在，当前为未跟踪文件；新增内容为上述实现。

内容指纹：`56db2caeaa93ab2f82134d2a258ae32b156c3b640b19cf3dc0ea99282f2b6414`。

### 136. UpdateState.java

文件：[src/main/java/com/ber/nimblePattern/pattern/UpdateState.java](../../src/main/java/com/ber/nimblePattern/pattern/UpdateState.java)。

状态：`=`；113 字节。

职责：UNTRACKED/LATEST/UPDATE 三状态，ordinal 被写入 NBT；改变顺序涉及存档语义。

改动：与比较基准无差异；保留上述既有实现，纳入整体审核，不虚构本轮修改。

内容指纹：`d50f4768246241dc97a134b7f64e92cb4018272a614bd988b6956e481628afc5`。

### 137. ExpectedAmount.java

文件：[src/main/java/com/ber/nimblePattern/probability/ExpectedAmount.java](../../src/main/java/com/ber/nimblePattern/probability/ExpectedAmount.java)。

状态：`??`；1843 字节。

职责：非负有理数量 record，BigInteger 约分、BigDecimal 小数转换、加/乘/除和严格整数化；防浮点舍入改变物料关系。

改动：基准中不存在，当前为未跟踪文件；新增内容为上述实现。

内容指纹：`3a09c401336782e530e4700218f680e1028a2dc33d2c796f83152891735dbd07`。

### 138. ProbabilityController.java

文件：[src/main/java/com/ber/nimblePattern/probability/ProbabilityController.java](../../src/main/java/com/ber/nimblePattern/probability/ProbabilityController.java)。

状态：`??`；16936 字节。

职责：每 CPU 概率 Attempt/remaining、byOutput、supplies、credits、工具需求、单个异步 Future 和 retryDebts；10 tick 查状态/补料，聚合同 key 补单，ACTIVE 优先集，save/load/clear。

改动：基准中不存在，当前为未跟踪文件；新增内容为上述实现。

内容指纹：`63db3224a0251269bc56deeb53ed45508310875dacc0a76f864790c2df393113`。

### 139. ProbabilityCpu.java

文件：[src/main/java/com/ber/nimblePattern/probability/ProbabilityCpu.java](../../src/main/java/com/ber/nimblePattern/probability/ProbabilityCpu.java)。

状态：`??`；165 字节。

职责：Mixin 能力接口：访问惰性 ProbabilityController，查询是否存在活跃概率控制器，避免普通路径触发分配。

改动：基准中不存在，当前为未跟踪文件；新增内容为上述实现。

内容指纹：`8554d06f60732e154475999e8041eee05f97bc60cee221b8ba29b30acde703ae`。

### 140. ProbabilityEncoding.java

文件：[src/main/java/com/ber/nimblePattern/probability/ProbabilityEncoding.java](../../src/main/java/com/ber/nimblePattern/probability/ProbabilityEncoding.java)。

状态：`??`；6865 字节。

职责：概率 NBT/相同内容重编码判断；normalize 初次期望倍率、hasProbabilityOutput 编辑保留身份、editedCycles 输入比例、ConfigInventory 快照。

改动：基准中不存在，当前为未跟踪文件；新增内容为上述实现。

内容指纹：`c594b2eb1cf1bc3e4276fd422c998839fc554d1325f252fc137ed767dc3a6add`。

### 141. ProbabilityJob.java

文件：[src/main/java/com/ber/nimblePattern/probability/ProbabilityJob.java](../../src/main/java/com/ber/nimblePattern/probability/ProbabilityJob.java)。

状态：`??`；317 字节。

职责：原 AE 运行任务的补单桥接契约：appendPlan 和 waiting 库存访问；实际操作由 ProbabilityJobMixin 实现。

改动：基准中不存在，当前为未跟踪文件；新增内容为上述实现。

内容指纹：`2040c8750ffcd421f7b8488d7aae79af2d6029306b54fc3e2daf5d6ab2f1cfe8`。

### 142. ProbabilityMachines.java

文件：[src/main/java/com/ber/nimblePattern/probability/ProbabilityMachines.java](../../src/main/java/com/ber/nimblePattern/probability/ProbabilityMachines.java)。

状态：`??`；4337 字节。

职责：目标接口、Tracked 标记、Ticket 计数；弱引用机器票据队列，完成次数一次分配；反射解析 GT 机器/唯一控制器，只接纳可靠 idle 归属。

改动：基准中不存在，当前为未跟踪文件；新增内容为上述实现。

内容指纹：`abedfb78a818910d17bcc9e02b2ac907cc8a88990d1a9ec69631d5b2744de586`。

### 143. ProbabilityPattern.java

文件：[src/main/java/com/ber/nimblePattern/probability/ProbabilityPattern.java](../../src/main/java/com/ber/nimblePattern/probability/ProbabilityPattern.java)。

状态：`??`；4546 字节。

职责：第三方样板概率标签恢复包装；Index 先检查有无概率标签，再按主产物索引实体输入输出；普通重复候选参与歧义拦截。

改动：基准中不存在，当前为未跟踪文件；新增内容为上述实现。

内容指纹：`aa28774ad92bef61c31334bd2a5e413a6926aecc14eb624b15db5598c854cfab`。

### 144. ProbabilityRecipes.java

文件：[src/main/java/com/ber/nimblePattern/probability/ProbabilityRecipes.java](../../src/main/java/com/ber/nimblePattern/probability/ProbabilityRecipes.java)。

状态：`??`；6799 字节。

职责：可选 GT/Create 输出概率适配及 JEI wrapper 解包；仅基础配方语义；OR/序列池权重、物品/流体转换、反射方法类级缓存。

改动：基准中不存在，当前为未跟踪文件；新增内容为上述实现。

内容指纹：`eb3e0d4b750a8bb028e3704d157100bdc1698004a32bfaffd4b27d383664fac7`。

### 145. ProbabilityTools.java

文件：[src/main/java/com/ber/nimblePattern/probability/ProbabilityTools.java](../../src/main/java/com/ber/nimblePattern/probability/ProbabilityTools.java)。

状态：`??`；290 字节。

职责：概率与工具/循环协调接口：查询持有工具量、预留工具、检测特殊结果仍待回收，避免同一物品被两个控制器抢收。

改动：基准中不存在，当前为未跟踪文件；新增内容为上述实现。

内容指纹：`37393461c17f657b2df9861c9041d1fa4f65315d317c451b68c07413b24ab42f`。

## 主资源与加载配置

### 146. pattern_tag_terminal.json

文件：[src/main/resources/assets/ae2/screens/terminals/pattern_tag_terminal.json](../../src/main/resources/assets/ae2/screens/terminals/pattern_tag_terminal.json)。

状态：`M`；文本差异 +16/-0；2677 字节。

职责：终端 AE 布局主文件：公共底图切片、共享槽、三页模式 panel/tab、搜索和滚动条；以 left/bottom 定位。

改动：新增 TOOL_INPUT 2 列虚拟槽、modePanel2/modeTabButton2、toolApplyButton；补循环三个槽的语义位置。

内容指纹：`d7b2855ce81f1b5b4261874a96679941976a961b2ed4d4a08f90febe5ba7963b`。

### 147. loop.json

文件：[src/main/resources/assets/ae2/screens/terminals/tag/loop.json](../../src/main/resources/assets/ae2/screens/terminals/tag/loop.json)。

状态：`M`；文本差异 +11/-3；249 字节。

职责：循环页独有按钮布局，loopApplyButton 左 129/底 112/40×15；背景由 Panel 代码绘制。

改动：从空槽/组件框架调整为循环 apply 按钮描述，清理占位配置。

内容指纹：`2645feff5b7709da2681ee5fe5695268a04e87826bdeecd205ac4d781e88790e`。

### 148. upgrade.json

文件：[src/main/resources/assets/ae2/screens/terminals/tag/upgrade.json](../../src/main/resources/assets/ae2/screens/terminals/tag/upgrade.json)。

状态：`=`；518 字节。

职责：更新页条件文本框、清除和应用按钮位置；由主布局 includes 引入。

改动：与比较基准无差异；保留上述既有实现，纳入整体审核，不虚构本轮修改。

内容指纹：`e99855004391f55cb3f2cc25fef2d60f819020f49f862caf710e2a40fe1b07e1`。

### 149. pattern_tag_terminal.json

文件：[src/main/resources/assets/nimble_pattern/models/item/pattern_tag_terminal.json](../../src/main/resources/assets/nimble_pattern/models/item/pattern_tag_terminal.json)。

状态：`=`；412 字节。

职责：终端物品模型，继承 ae2:item/display_base，映射正面及 bright/medium/dark 图层。

改动：与比较基准无差异；保留上述既有实现，纳入整体审核，不虚构本轮修改。

内容指纹：`c7ac73d3fe8f107e312f374cfe0cdada1aba593fab6180a39389e404eee8d599`。

### 150. pattern_tag_terminal_off.json

文件：[src/main/resources/assets/nimble_pattern/models/part/pattern_tag_terminal_off.json](../../src/main/resources/assets/nimble_pattern/models/part/pattern_tag_terminal_off.json)。

状态：`=`；276 字节。

职责：终端断电部件模型，继承 ae2:part/display_off，映射三层终端材质。

改动：与比较基准无差异；保留上述既有实现，纳入整体审核，不虚构本轮修改。

内容指纹：`3aa0567345871a16ce85c073de32252e45bf5043c5bfef2ca6f7bc451f6dae51`。

### 151. pattern_tag_terminal_on.json

文件：[src/main/resources/assets/nimble_pattern/models/part/pattern_tag_terminal_on.json](../../src/main/resources/assets/nimble_pattern/models/part/pattern_tag_terminal_on.json)。

状态：`=`；1123 字节。

职责：终端通电部件模型，三层发光面及 tintindex 3/2/1；使用 PartModels 的运行状态组合。

改动：与比较基准无差异；保留上述既有实现，纳入整体审核，不虚构本轮修改。

内容指纹：`b071ebaefd6196504105a4290df03ca06ec2900c233b5956834e430cda59691b`。

### 152. button_disabled.png

文件：[src/main/resources/assets/nimble_pattern/textures/gui/button_disabled.png](../../src/main/resources/assets/nimble_pattern/textures/gui/button_disabled.png)。

状态：`=`；226 字节。

职责：NimbleButton 禁用背景，与禁用文字配色配合。

改动：与比较基准无差异；保留上述既有实现，纳入整体审核，不虚构本轮修改。

内容指纹：`d45e7b6314c216b81489f576c3c06e736d34502ef3c6db1430308e1bb34227f2`。

### 153. button_highlighted.png

文件：[src/main/resources/assets/nimble_pattern/textures/gui/button_highlighted.png](../../src/main/resources/assets/nimble_pattern/textures/gui/button_highlighted.png)。

状态：`=`；224 字节。

职责：NimbleButton 悬停或焦点背景，保持同一边缘切片尺寸。

改动：与比较基准无差异；保留上述既有实现，纳入整体审核，不虚构本轮修改。

内容指纹：`5f2bda04a241ebfd4dac3799c42b413c2653f6607757c95e41d86eb73b3f7ca9`。

### 154. button.png

文件：[src/main/resources/assets/nimble_pattern/textures/gui/button.png](../../src/main/resources/assets/nimble_pattern/textures/gui/button.png)。

状态：`=`；229 字节。

职责：NimbleButton 普通态背景，200×20 参考尺寸按九宫格拉伸。

改动：与比较基准无差异；保留上述既有实现，纳入整体审核，不虚构本轮修改。

内容指纹：`5e815b235384d3ad4422e61e044804c097e486ffdb61fade6056965069c8f382`。

### 155. loop_mode.png

文件：[src/main/resources/assets/nimble_pattern/textures/guis/loop_mode.png](../../src/main/resources/assets/nimble_pattern/textures/guis/loop_mode.png)。

状态：`=`；540 字节。

职责：循环页材质：入口、出口和元件的框与箭头；实际可点击槽由 Menu 注册/ScreenStyle 定位。

改动：与比较基准无差异；保留上述既有实现，纳入整体审核，不虚构本轮修改。

内容指纹：`cf6a0a15d8aeebd1312b8c58665ba857f45d287dd0f5cdfd955a1d8693f6d13d`。

### 156. pattern_tag_terminal.png

文件：[src/main/resources/assets/nimble_pattern/textures/guis/pattern_tag_terminal.png](../../src/main/resources/assets/nimble_pattern/textures/guis/pattern_tag_terminal.png)。

状态：`=`；3378 字节。

职责：公共终端 GUI 基底（头部/重复行/底部），所有页共用；不是旧单页完整材质。

改动：与比较基准无差异；保留上述既有实现，纳入整体审核，不虚构本轮修改。

内容指纹：`0329d739d62bb86f25601cd98f9bd7fd1efa1f4e8628703c9977e3eb4b351e0e`。

### 157. pattern_upgrade_terminal_old.png

路径：`src/main/resources/assets/nimble_pattern/textures/guis/pattern_upgrade_terminal_old.png`（已删除）。

状态：`D`；文本差异 +-/--。

职责：旧单页 GUI 材质留存文件。

改动：删除过时资源；当前使用 pattern_tag_terminal 基底与各模式独立材质。此次审核不恢复该文件。

### 158. loop.png

文件：[src/main/resources/assets/nimble_pattern/textures/guis/tabs/loop.png](../../src/main/resources/assets/nimble_pattern/textures/guis/tabs/loop.png)。

状态：`??`；408118 字节。

职责：透明循环图标：首尾相接双箭头，内置 imagegen 输出，1254×1254 原图在 GUI 缩至 16×16。

改动：基准中不存在，当前为未跟踪文件；新增内容为上述实现。

内容指纹：`06bcad76731c8842d7e268d96c7ee7536ced7dcbd7d66faae31e8c58b2acd024`。

### 159. upgrade.png

文件：[src/main/resources/assets/nimble_pattern/textures/guis/tabs/upgrade.png](../../src/main/resources/assets/nimble_pattern/textures/guis/tabs/upgrade.png)。

状态：`??`；358914 字节。

职责：透明更新图标：浅灰白上箭头+底横线，内置 imagegen 输出，1290×1219 原图在 GUI 缩至 16×16。

改动：基准中不存在，当前为未跟踪文件；新增内容为上述实现。

内容指纹：`2460c4309a8f3b33f81dfe2a230e25f4aa52975c3d1f24c4b04919e3d03207f5`。

### 160. tool_mode.png

文件：[src/main/resources/assets/nimble_pattern/textures/guis/tool_mode.png](../../src/main/resources/assets/nimble_pattern/textures/guis/tool_mode.png)。

状态：`??`；494 字节。

职责：工具页四个框的专用材质，PatternToolPanel 绘制；图片本身不执行工具校验。

改动：基准中不存在，当前为未跟踪文件；新增内容为上述实现。

内容指纹：`04df8eb8ece87afa82b73966ab2d08d18a2e7ff9cd6cdc1c616c2851ae70636f`。

### 161. upgrade_mode.png

文件：[src/main/resources/assets/nimble_pattern/textures/guis/upgrade_mode.png](../../src/main/resources/assets/nimble_pattern/textures/guis/upgrade_mode.png)。

状态：`=`；406 字节。

职责：更新页 88×68 逻辑区域材质，PatternUpgradePanel 的 BG 使用全区域。

改动：与比较基准无差异；保留上述既有实现，纳入整体审核，不虚构本轮修改。

内容指纹：`3cb2049c83858ae14fc51e0c83a6acd816753337cb35293d4b2e494bab369d77`。

### 162. loop_storage_cell_16k.png

文件：[src/main/resources/assets/nimble_pattern/textures/item/loop_storage_cell_16k.png](../../src/main/resources/assets/nimble_pattern/textures/item/loop_storage_cell_16k.png)。

状态：`=`；365 字节。

职责：16k 循环元件独立物品贴图，物品模型引用；容量规则在枚举/Item 中，不从文件名或像素推导。

改动：与比较基准无差异；保留上述既有实现，纳入整体审核，不虚构本轮修改。

内容指纹：`9205461d9cd757afc9b7713ad4eacfa332cca11afbd18c3a74c23c3070f38aad`。

### 163. loop_storage_cell_1k.png

文件：[src/main/resources/assets/nimble_pattern/textures/item/loop_storage_cell_1k.png](../../src/main/resources/assets/nimble_pattern/textures/item/loop_storage_cell_1k.png)。

状态：`=`；362 字节。

职责：1k 循环元件独立物品贴图，物品模型引用；容量规则在枚举/Item 中，不从文件名或像素推导。

改动：与比较基准无差异；保留上述既有实现，纳入整体审核，不虚构本轮修改。

内容指纹：`8ab3c761c2a13f74bc1de9956b3b8b4045816aee6a371ed4ac23eefebd628d28`。

### 164. loop_storage_cell_256k.png

文件：[src/main/resources/assets/nimble_pattern/textures/item/loop_storage_cell_256k.png](../../src/main/resources/assets/nimble_pattern/textures/item/loop_storage_cell_256k.png)。

状态：`=`；361 字节。

职责：256k 循环元件独立物品贴图，物品模型引用；容量规则在枚举/Item 中，不从文件名或像素推导。

改动：与比较基准无差异；保留上述既有实现，纳入整体审核，不虚构本轮修改。

内容指纹：`95bc99ed28397734cf8737856c4f6dcef421807a3f9bcbb2b6c1c3fff69fafaf`。

### 165. loop_storage_cell_4k.png

文件：[src/main/resources/assets/nimble_pattern/textures/item/loop_storage_cell_4k.png](../../src/main/resources/assets/nimble_pattern/textures/item/loop_storage_cell_4k.png)。

状态：`=`；361 字节。

职责：4k 循环元件独立物品贴图，物品模型引用；容量规则在枚举/Item 中，不从文件名或像素推导。

改动：与比较基准无差异；保留上述既有实现，纳入整体审核，不虚构本轮修改。

内容指纹：`09bf559fc04ea14699859a030baa5b6c442c72f6263943d3c29d8b6288e1cb58`。

### 166. loop_storage_cell_64k.png

文件：[src/main/resources/assets/nimble_pattern/textures/item/loop_storage_cell_64k.png](../../src/main/resources/assets/nimble_pattern/textures/item/loop_storage_cell_64k.png)。

状态：`=`；364 字节。

职责：64k 循环元件独立物品贴图，物品模型引用；容量规则在枚举/Item 中，不从文件名或像素推导。

改动：与比较基准无差异；保留上述既有实现，纳入整体审核，不虚构本轮修改。

内容指纹：`14255a4b5a3de9b3b6f039a83fcb323651746f77d4563b8e2b3862aefc4c6808`。

### 167. pattern_tag_terminal_bright.png

文件：[src/main/resources/assets/nimble_pattern/textures/part/pattern_tag_terminal_bright.png](../../src/main/resources/assets/nimble_pattern/textures/part/pattern_tag_terminal_bright.png)。

状态：`=`；192 字节。

职责：终端部件_bright图层，供物品/部件模型映射并按 AE tint/亮度渲染；与 GUI 面板材质不同。

改动：与比较基准无差异；保留上述既有实现，纳入整体审核，不虚构本轮修改。

内容指纹：`b966413f31d94b9ea56e793f7060ae5da189e167291f7bd1979ba1bff55f2e0a`。

### 168. pattern_tag_terminal_dark.png

文件：[src/main/resources/assets/nimble_pattern/textures/part/pattern_tag_terminal_dark.png](../../src/main/resources/assets/nimble_pattern/textures/part/pattern_tag_terminal_dark.png)。

状态：`=`；176 字节。

职责：终端部件_dark图层，供物品/部件模型映射并按 AE tint/亮度渲染；与 GUI 面板材质不同。

改动：与比较基准无差异；保留上述既有实现，纳入整体审核，不虚构本轮修改。

内容指纹：`d3dc992f015da34cea59524ac2aa901229bc8aebb3301ab4b62824838ac77510`。

### 169. pattern_tag_terminal_medium.png

文件：[src/main/resources/assets/nimble_pattern/textures/part/pattern_tag_terminal_medium.png](../../src/main/resources/assets/nimble_pattern/textures/part/pattern_tag_terminal_medium.png)。

状态：`=`；156 字节。

职责：终端部件_medium图层，供物品/部件模型映射并按 AE tint/亮度渲染；与 GUI 面板材质不同。

改动：与比较基准无差异；保留上述既有实现，纳入整体审核，不虚构本轮修改。

内容指纹：`6e279bd8b08dcfe40d2c79d7e33d1ad90666d723bc4180adbf297865699af11c`。

### 170. pattern_tag_terminal.png

文件：[src/main/resources/assets/nimble_pattern/textures/part/pattern_tag_terminal.png](../../src/main/resources/assets/nimble_pattern/textures/part/pattern_tag_terminal.png)。

状态：`=`；244 字节。

职责：终端部件基底图层，供物品/部件模型映射并按 AE tint/亮度渲染；与 GUI 面板材质不同。

改动：与比较基准无差异；保留上述既有实现，纳入整体审核，不虚构本轮修改。

内容指纹：`e8cb2f6826434d880d2bb16a66311eba4c86023f6fba96d9e5ef0d950bf34c2a`。

### 171. accesstransformer.cfg

文件：[src/main/resources/META-INF/accesstransformer.cfg](../../src/main/resources/META-INF/accesstransformer.cfg)。

状态：`=`；114 字节。

职责：去除/开放 Minecraft Slot.x/y 的 final 限制，允许 Screen 重定位共享槽和滚动显示。

改动：与比较基准无差异；保留上述既有实现，纳入整体审核，不虚构本轮修改。

内容指纹：`3a895189a756c6979104331bb3c115faec4ebfadf5db228c0eb28fbb013f83df`。

### 172. mods.toml

文件：[src/main/resources/META-INF/mods.toml](../../src/main/resources/META-INF/mods.toml)。

状态：`=`；5380 字节。

职责：Forge 模组/依赖版本约束与两份 Mixin 配置声明；可选兼容目标范围不等于每个版本都验证过。

改动：与比较基准无差异；保留上述既有实现，纳入整体审核，不虚构本轮修改。

内容指纹：`dcf02fc03da051aa3fc9243298366bdaabca97a13c4b3e6732569c4311e343be`。

### 173. nimble_pattern.extendedae.mixins.json

文件：[src/main/resources/nimble_pattern.extendedae.mixins.json](../../src/main/resources/nimble_pattern.extendedae.mixins.json)。

状态：`=`；368 字节。

职责：ExtendedAE 可选 PartExPatternProviderMixin 配置，required=false/defaultRequire=0，隔离可选供应器部件升级支持。

改动：与比较基准无差异；保留上述既有实现，纳入整体审核，不虚构本轮修改。

内容指纹：`01e9973fe0396af275b26c41c69c20a979a69fae9e4e67d180074c256fc2150b`。

### 174. nimble_pattern.mixins.json

文件：[src/main/resources/nimble_pattern.mixins.json](../../src/main/resources/nimble_pattern.mixins.json)。

状态：`M`；文本差异 +17/-0；1300 字节。

职责：主 Mixin 清单、client-only 清单、JAVA_17/refmap/defaultRequire=1；决定功能是否实际注入。

改动：追加概率编码/追踪/CPU/补单/优先、GTL 批次、循环规划/供应器恢复、工具计划/重载及 accessor；保持主配置 required=true。

内容指纹：`4d6ce49878559c62079e8c71a93762ea36b68ae5fe94c0a1aca9fd29811ccf0a`。

### 175. pack.mcmeta

文件：[src/main/resources/pack.mcmeta](../../src/main/resources/pack.mcmeta)。

状态：`=`；95 字节。

职责：资源包元数据和构建占位符，随 processResources 展开；不是某个页签的具体布局。

改动：与比较基准无差异；保留上述既有实现，纳入整体审核，不虚构本轮修改。

内容指纹：`93a07565df9a67c93b3926aa2a2fbe6fad990359c48f438e540ece9b99f5d830`。

## 测试

### 176. LoopBatchSizingTest.java

文件：[src/test/java/com/ber/nimblePattern/crafting/LoopBatchSizingTest.java](../../src/test/java/com/ber/nimblePattern/crafting/LoopBatchSizingTest.java)。

状态：`??`；1572 字节。

职责：独立 main 算术回归，不自动计入 JUnit/GameTest；验证增殖批次、足种子大批、边界及总净收益/耗材守恒。

改动：基准中不存在，当前为未跟踪文件；新增内容为上述实现。

内容指纹：`6eceafe74b43f861a9431828af041ed5c3527f1425d2c21236bd7b9e9a65ddb0`。

### 177. OptimizationGameTests.java

文件：[src/test/java/com/ber/nimblePattern/crafting/OptimizationGameTests.java](../../src/test/java/com/ber/nimblePattern/crafting/OptimizationGameTests.java)。

状态：`??`；16993 字节。

职责：13 项优化/正确性回归：身份、native 映射、缓存扫描计数、多网络更新、模糊债务、五档枚举、概率批数/歧义/票据清理。

改动：基准中不存在，当前为未跟踪文件；新增内容为上述实现。

内容指纹：`fb03a0e557fe4f0fa00c31fdecf19972aca22b6bf1805a180c4399b933a13682`。

### 178. ProbabilityGameTests.java

文件：[src/test/java/com/ber/nimblePattern/crafting/ProbabilityGameTests.java](../../src/test/java/com/ber/nimblePattern/crafting/ProbabilityGameTests.java)。

状态：`??`；21058 字节。

职责：13 项概率回归：期望/编辑/删催化剂、票据归属、标记切换、存档/取消、同 CPU 缺料补料、批次合并、开发 GT API、恢复身份。

改动：基准中不存在，当前为未跟踪文件；新增内容为上述实现。

内容指纹：`f082c452ca24c2c8ea9b8ff93aa020247d588a21b7fcf73af2b9e454d076f873`。

### 179. ToolPatternGameTests.java

文件：[src/test/java/com/ber/nimblePattern/crafting/ToolPatternGameTests.java](../../src/test/java/com/ber/nimblePattern/crafting/ToolPatternGameTests.java)。

状态：`??`；16214 字节。

职责：6 项工具 GameTest：不可消耗工具合法性、余物等待、模板净增与 100 次复用、计划预留、CPU/重载、取消和拒绝回滚。

改动：基准中不存在，当前为未跟踪文件；新增内容为上述实现。

内容指纹：`6e82839fc533a0852412ced45006fa1bcacf0e9c78d4ec6fcd396ab9d41c92da`。

### 180. empty.snbt

文件：[src/test/structures/empty.snbt](../../src/test/structures/empty.snbt)。

状态：`??`；87 字节。

职责：GameTest 共用空结构模板，prepareToolTestStructures 将其复制到独立 run-gametest/gameteststructures。

改动：基准中不存在，当前为未跟踪文件；新增内容为上述实现。

内容指纹：`91d257cd50d1d84dc4c52d8ea99b9dcba27732fc3ec8efb40f3feaa635f5fb67`。

## 本套审核文档自身

| 文件 | 本次新增内容 |
| --- | --- |
| [README.md](README.md) | 基线、证据口径、导航、环境与全局不变量 |
| [01-core-fuzzy-fake.md](01-core-fuzzy-fake.md) | 统一包装、执行身份、供应器映射、模糊输入/输出、假完成 |
| [02-terminal-upgrade.md](02-terminal-upgrade.md) | 终端分层/布局、槽/同步/搜索、来源回填、更新追踪和新图标 |
| [03-loop-storage.md](03-loop-storage.md) | 解析限制、计划折叠、种子/批数/取消、存储权限/容量/可见性 |
| [04-tool.md](04-tool.md) | 工具校验、计划一次预留、四库存执行、取消/保存 |
| [05-probability.md](05-probability.md) | 来源和概率适配、期望/编辑、机器票据、超时/补单/缺料 |
| [06-optimization.md](06-optimization.md) | 逐项优化、失效边界、复杂度、基准测量建议 |
| [07-integration-contracts.md](07-integration-contracts.md) | 全部 Mixin 注入表、协议方向、NBT、线程生命周期 |
| [08-validation-risks.md](08-validation-risks.md) | 32 项已有测试覆盖、运行命令、明确风险和手工验收 |
| [09-file-inventory.md](09-file-inventory.md) | 逐文件责任与差异、删除项、内容指纹和文档本身范围 |

## 审核提交前的覆盖检查

1. `git diff HEAD --name-only` 与 `git ls-files --others --exclude-standard` 的业务/资源/测试文件都应能在本清单定位。
2. 未跟踪目录不会出现在普通 `git diff --stat` 中；不能仅审 41 个已跟踪改动而漏掉整个 crafting/probability 目录。
3. 五个旧元件类和旧包装类删除必须与调用替换一起审核；不能只加入新类而不处理老引用。
4. Linux 提交需特别校验 Nimblepattern.java/NimblePattern.java 大小写；本清单按实际文件系统名称列出。
5. 修改任何源码后内容指纹会过期，发布审核应刷新快照，或明确附上新差异。

