# 02 样板标记终端、更新条件及图形界面

## 1. 分层与库存所有权

`PatternTagTerminalPart` 是 AE 显示部件与菜单宿主，持有 `PatternTagLogic`。Logic 持有持久化库存和上次模式；Menu 操作服务端库存并同步；Screen/Panel 只负责显示与发送操作。

| 库存 | 大小 | 槽语义 | 是否真实物品 |
| --- | --- | --- | --- |
| inputPattern | 3×27=81 | INPUT_PATTERN | 是，只接受编码样板，每槽 1 |
| conditionItem | 1 | CONDITION_ITEM | FakeSlot |
| loopEndpoints | 2 | LOOP_INPUT、LOOP_OUTPUT | FakeSlot，可表示物品/流体 |
| loopStorageCell | 1 | LOOP_LOCK_STORAGE_CELL | 是，仅 LoopStorageCellItem |
| tools | 4 | TOOL_INPUT | FakeSlot；应用时只接受 AEItemKey |

左侧样板输入在界面同时显示 3×3 槽，独立滚动条访问其他行。真实槽由 Menu 注册；Screen 统一定位及隐藏不可见行，不能放进某个 Panel，否则切页会失去公共区域。

部件拆除将 inputPattern 与 loopStorageCell 加入掉落；虚拟工具和端点不生成物品。clearContent 清空样板、端点、工具、元件库存；conditionItem 没有显式清空，属于现状，不应称为全部虚拟槽均被清空。

## 2. 记住页签

TagMode 顺序为 UPGRADE、LOOP、TOOL。Logic 默认 UPGRADE，setMode 变化时 markForSave；NBT 保存枚举 name 到 mode，非法/旧值恢复为 UPGRADE。

Menu 从 Logic 初始化 mode，使用 @GuiSync(97) 同步；客户端 setMode 发 AE client action，服务端修改 Logic。broadcastChanges 会读取共享 Logic 的最新模式。

记忆属于**同一个终端部件**，不是每个玩家的个人首选项。多人打开同一部件可能互相影响当前模式。

## 3. Panel、材质和页签

- Screen 绘制可变行数的 header/firstRow/row/lastRow/bottom、网络样板列表、公共输入槽和滚动条。
- TagModePanel 定义位置、可见性、88×68 边界、createTabButton 与 tooltip 契约。
- UpgradePanel 绘制 upgrade_mode.png；维护条件文字/虚拟物品双向同步、历史补全、应用与清除按钮。
- LoopPanel 绘制 loop_mode.png；定位端点与元件槽、调用 applyLoop。
- ToolPanel 绘制 tool_mode.png；定位四个工具槽、调用 applyTools。
- Panel 切换同时控制按钮 visible 和槽位隐藏，避免非活动页响应点击。

当前布局以源码 JSON 为准，不能直接套早期需求中的 bottom:104：

| 对象 | left | bottom / top | 尺寸或说明 |
| --- | --- | --- | --- |
| 三个 modePanel | 88 | bottom 166 | 88×68 内容 |
| 更新/循环/工具页签 | 175 | bottom 174 / 153 / 132 | 22×22 |
| 公共输入槽 | 25 | bottom 157 | 3 列 |
| 公共输入滚动条 | 15 | bottom 158 | 高 52 |
| 循环入口/工具左上槽 | 98 | bottom 157 | 对齐 |
| 循环出口 | 98 | bottom 121 | 入口下方 |
| 循环元件槽 | 148 | bottom 157 | 右上 |
| 三页应用按钮 | 129 | bottom 112 | 40×15 |
| 更新清除按钮 | 129 | bottom 135 | 40×15 |

图标最近一轮修改：两页原版物品图标改为独立透明 PNG；TextureTabButton 继承 AE TabButton，先画原背景，再按 HORIZONTAL 偏移 (1,3) 画 16×16 图标。BOX/CORNER 分别使用 x 偏移 3/4。保留选择、焦点、tooltip 与按键叙述。ToolPanel 仍用下界合金升级锻造模板物品。

PNG 为生成图原始分辨率，不是原生 16×16 文件；Blitter 参考宽高为逻辑尺寸，完整 UV 缩放显示。生成方式和提示词见 ../tab-icons.md；显存/缩放清晰度应在实际资源包和 GUI scale 下验收。

## 4. 网络样板浏览同步

服务端 Menu 遍历当前活动网格中实现 PatternContainer 的机器，建立容器身份 → ContainerTracker，以及 serverId → tracker。serverId 是当前会话临时序号，不是存档身份。

完整同步：

1. ClearPacket 清除客户端记录并进入 fullUpdateInProgress。
2. 每个容器发 full PatternPacket，携带库存大小及非空槽。
3. PatternSyncCompletePacket 到达后一次性筛选、排序和重建显示。
4. 条件历史独立通过 ConditionPacket 更新。

增量同步每 5 tick 检查，先用 PatternInventorySnapshots 的 revision 判断是否变化；版本变了才比较本菜单上次已发出的各槽副本，只发变化槽，包括清空槽。conditionRefCounts 增减引用计数；conditionsDirty 才重新排序和同步历史。

客户端增量包先更新记录并设置 viewDirty，一帧前合并 rebuildPatternView。可见槽只在数据、滚动位置、行数或布局改变时重建；不是每次 drawFG 都重新分配。

注意共享快照不代表变化库存只扫描一遍：快照先扫描，检测到版本变化的各菜单仍会做自己的槽差异比较。

## 5. 浏览和搜索

PatternContainerRecord 持有客户端显示库存；PatternRecord 缓存主产物名称的 sortKey。PatternUpgradeSlot 仅代理显示与 serverId/来源槽位，不允许普通 Slot.set/remove/pickup 路径修改真实库存。

搜索不是全输入/全输出扫描：UnwrapHelper 取编码样板显示的主产物，解包流体后搜索。

- 空格：AND；竖线：OR。
- @：模组 ID/名称；#：tooltip；$：tag；*：物品/流体 ID。
- %：更新条件（注册 ID 可转换为显示名）；~：UNTRACKED/0、LATEST/1、UPDATE/2。
- 普通词：主产物名称；JECharacters 存在时支持拼音匹配。
- tooltip 弱键缓存、tag 按 key 类型缓存；文本变更才重新解析查询。
- 旧英文搜索提示写 ~UPGRADE，但 StatusSearchPredicate 接受 UPDATE；这是需核对的文案不一致，不应在文档中掩盖。

## 6. 来源记录与回填

从网络样板列表取出实体样板时，NimblePatternTag.tagSource 保存 dim、pos、side、slot。side=6 表示方块，其他为部件方向。普通左/右键、shift 取出都在服务端处理。

pushPatternBack：

1. 确认非空且为编码样板。
2. 解码来源维度和位置，区块未加载直接失败，不强制加载。
3. 解析 PatternContainer、线缆部件或 ExtendedAE 部件；GT 尝试 getMetaMachine 和多个库存访问器，必要时适配 LDlib ItemStackTransfer。
4. 目标槽存在且为空才回填。
5. 回填前去掉来源；update 仅在 UNTRACKED 时去掉，保留有效管理标签及执行标签。

更新/循环/工具应用都采用“能回则回，不能则把已标记实体留在终端”的行为。没有找到来源不是标记失败。

重要边界：当前回填是按坐标+槽定位，不是按来源网络/拥有者鉴权；反射适配器写入异常可能被吞掉；详见风险表。不要把一次布尔成功等同于所有第三方库存都具有可证明的原子回填。

## 7. 更新样板实际做什么

applyCondition 给左侧每个非空样板副本写 update.condition 与 status=LATEST，随后尝试回填。clearCondition 清除 condition 虚拟槽，并去掉每个输入样板的 update 子树，不删除其他功能标签。

更新功能只是**标记待人工更新和提醒**，不会自动重编写配方。

- 可用虚拟物品/流体的注册 ID 作为条件，也可手写文本。
- 只有能解析为已注册物品/流体/方块 ID 的条件进入自动追踪。
- 手写普通文本可用于管理和搜索，但没有自动达成判断。
- 状态 ordinal：UNTRACKED=0、LATEST=1、UPDATE=2；非法状态读取为 UNTRACKED。

## 8. 全服追踪与提醒

NetworkStorageMixin.insert RETURN 只在 MODULATE 且实际插入量>0 时提交 what.getId；不以 SIMULATE 或请求投料数量触发。

PatternUpgradeTracker 每 20 个服务器 END tick：

1. 遍历活动网格内 PatternContainer，按库存对象去重。
2. 强制刷新共享快照；仅 revision 改变的库存重建条件 → 槽位索引。
3. 移除不再活动的库存索引。
4. 对 pendingIds 去重集合逐个处理；再次核对槽当前条件及状态。
5. 只复制确实要修改的样板，标为 UPDATE，并汇总数量。
6. 给携带“有电且已绑定”的无线终端玩家发送通知；不核实其绑定的网络是否相同。
7. 客户端正打开 PatternTagTermScreen 时抑制 toast，其他情况显示约 2.5 秒。

语义是**任一网络发生符合 ID 的真实入网，更新全服活动网络的匹配条件**。这保留了原语义，不是网络隔离的触发器；新增带标签样板会在下个轮询纳入，所以事件即使暂时未被索引也先保留。

服务器启动/停止 clear 索引与快照。性能为周期轮询而非真正零扫描事件驱动；大量容器仍有可观周期成本。
