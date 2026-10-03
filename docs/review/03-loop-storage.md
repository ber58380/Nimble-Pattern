# 03 循环样板与循环存储元件

## 1. 标记入口与事务边界

PatternTagTermMenu.applyLoop 读取入口、出口 FakeSlot 和真实元件槽，复制所有非空输入样板并保留原槽索引。必须有 LoopStorageCellItem；取得/创建元件 UUID，然后调用 LoopPatternParser.parse。

解析成功后先 canAddConfiguredAmount 验证容量，再向副本写 loop 标签，给元件**累加**入口标记量，最后按解析结果的 sourceIndices 回填原槽或来源供应器。解析或容量错误不会提交样板标签和数量；但 getOrCreateCellId 在解析前执行，失败可能已经给元件增加 UUID，不能声称整个流程完全无写入。

成功不会强制要求样板供应器仍存在。ArithmeticException 转为 amount_overflow 提示。

## 2. 找环算法与真实限制

解析器是用户辅助的**唯一线性闭环**解析，不是通用有向图或任意化学计量求解器。

1. 每个样板必须解码为 AEProcessingPattern，取 outputs[0] 为主产物。
2. 以每个输入 possibleInputs[0] × multiplier 聚合数量，保留精确 AEKey。
3. 数量至少 2；入口和出口必须分别出现在某个主产物中。
4. 从入口 key 出发，在剩余样板中查找包含该输入 key 的唯一样板。没有为 broken_path，多于一个为 ambiguous_path。
5. 第一条边需要的入口数量就是 seedAmount。
6. 每一步主产物数量必须恰好等于下一步消耗链上材料的数量，否则 unbalanced_intermediate；不自动求配方倍数。
7. 每步除当前链材料外的输入累加入 externalInputs。
8. 再次产出入口时结束，所有样板必须都被使用。
9. 倒数第二步主产物必须等于玩家指定的出口。
10. 最后入口产出必须大于种子量，netOutput=末步产出-seedAmount。

这意味着分支、多个配方都消耗同一个中间物、跨步复用早先的环内产物、相邻数量不相等都不属于已支持模型。“额外输入”按上述局部规则计算，不是对所有内部物料做全局消元。副产物不参与环的路径和净收益计算。

### 数据

每组生成 groupId，每张样板复制同一份入口/出口、元件 UUID、seed/netOutput、externalInputs、mainOutputs，并记录自身 index 和 size。完整键结构见 07。每个样板都携带全环描述，成本约为 O(n²) 的重复元数据；这是当前设计，尚未集中到组级存储。

旧单样板环解析和运行均拒绝；锻造模板增殖走工具样板，不走本功能。

## 3. 为什么 AE 不再因入口种子形成递归

NimbleEncodedPattern 仅将闭环最后一张样板视为 composite：

- 对 AE 广告输入：externalInputs。
- 对 AE 广告输出：entry × netOutput。
- 环内前缀样板：仍广告原输入/输出。

例如 A→B、B+D→C、C→2A，AE 看到末步是 D→A，而前缀 A→B、B+D→C 仍可规划。用户要 B 时，若普通库存没有可用 A，AE 先执行 D→A 的完整环，再用其净增产 A 执行前缀得到 B。不是把保留种子直接转成 B。

如果网络本来有普通可用 A，AE 可以直接使用它执行前缀；当前实现并不强制所有环内物品订单都无条件再跑整环。保护的是循环元件种子不被普通计划花掉。

### 种子检查和库存隔离

NetworkCraftingSimulationStateMixin 在创建网络快照后重建可用量，排除循环元件；必要时按 AE 设置做模拟抽取。即使终端可显示“只有循环元件时的种子”，普通材料计划也不会消费它。

CraftingTreeProcessMixin.request 对 composite 检查指定 UUID 元件里是否有一份 seedAmount：

- 正常计算缺种子抛 CraftBranchFailure。
- 缺料模拟通过 addMissing 仅报告一次种子。
- 不把种子乘以计划轮数，因为后续可以增殖复用。

该检查直接访问活网格/元件，不是仅访问不可变快照；合成计算线程安全是高优先级审核项。

## 4. 真实执行状态机

LoopCraftingController 在 CPU 内按需创建，每 CPU 同时一条活动环。working 是控制器真实暂存库存；steps 为已恢复的同组样板序列；expectedKey/expectedRemaining 表示当前主产物等待。

### start

- 拒绝重入、无网络、非正操作数。
- 用 Math.multiplyExact 预检计划输出、每步输入/输出的倍乘溢出。
- 按 groupId/index 从 CraftingService 的产物索引解析所有步骤。
- 从指定循环元件真实抽取一份种子；没有种子不启动。
- 将本次 AE 提供的外部材料复制到 working，加上种子，记录总操作数和总净输出。

此处依赖 AE 在 push 成功后转移/清理 holders 的契约；不能另外重复扣一次网络库存。

### tick 与批处理

有当前输出未收齐时不派发下一步。每次开始一轮整环批次：

`batchOperations = min(remainingOperations, floor(availableSeed / seedPerOperation))`

ScaledPattern 只用于放大输入抽取和输出计数；实际供应器收到其注册的原始 pattern 与放大 holders。先模拟电量，再遍历非忙供应器；成功才实际扣 AE 电。全部拒绝则将抽出的 holders 放回 working，清空当前等待。

数量足够时一次派发剩余全部轮数；不足时先增殖：

- 需求 10 次、种子 1、每次净增 1：批次 1、2、4、3。
- 种子已有 10：批次 10。
- 每次净增 8，需求只是 2 次，初始一份：1、1。

增殖轮数本身属于订单，不是额外执行；外部耗材总量仍等于计划次数。每个已确定批次内，所有步骤使用同一个倍率。

### acceptOutput

只收当前精确 expectedKey，最多收 expectedRemaining；SIMULATE 不变更。收齐后推进 stepIndex。整环一批完成则减少 remainingOperations；还有剩余则保留净收益作更多种子，重新计算下一批。

最终完成先将原始 seedAmount 归还启动时持有的元件对象，再从 working 取总净产出，结束控制器。CPU 对直接最终目标的交付需要“真实库存 + 独立结账”分离，具体在 01 说明，避免 16 个实际产物被结账扣掉 10 个只剩 6 个回网。

当前只跟踪主产物，expectedContainers 和额外输出未进入环步骤等待账本。复杂余物、副产物回流需实际审核。

## 5. GTL 总成和批次数

GTL 可能重编码实体样板、删去集成电路或副产物，导致自定义 NBT 丢失：

- ProviderState 从 PatternContainer 的真实样板库存收集 loopData。
- 根据对外主产物 key+amount 匹配，多个不同候选时不猜测。
- 原始供应器身份由 ProviderPatternIndex 保存。

GtlLoopBatchMixin 从 CraftingPatternAutoExpand.getOperations 的 RETURN 捕获真实派发次数；LoopBatchDispatch 使用 ThreadLocal 保存 pattern 对象引用与次数，后续 CPU.push 只在同一对象时取出，否则默认 1，并立即清除。executeCrafting 开始及取消也清理，防止跨调用泄漏。

这解决“不知道 GTL 已把材料乘了批数、仍按一轮计账”的问题。实际 GTL 覆盖方法、Mixin 命中和大批 holders 是否被具体总成接受仍需实机测试。

## 6. 取消与失败

cancelAndReturnSeed：

1. 如果 working 仍有完整原始种子，并且原元件可接收，则归还。
2. 若 seed 已转成中间产物或留在机器，返回 lostSeed key。
3. 未派发的 working 内容回到 CPU 库存，由 AE 取消流程处理。
4. resetFinishedState 清掉 active、steps、cell、expected、批次数与库存。

CPU 向仍在线的发起玩家发 LoopSeedLostNotificationPacket；没有可定位玩家（如某些 crafting-card 任务）不会显示同样通知。已经进机器的材料不撤回；迟到产物按网络的后续正常路径处理。

限制必须正视：

- 当前 LoopCraftingController 没有 save/load；CPU 只保存工具、模糊和概率控制器，活动环跨重启不能认为安全。
- 最终种子归还失败后保持 active，但 stepIndex 已到末尾，tick 提前返回；未看到独立重试归还状态。修改元件配置、卸载/移除设备时需专门测试。
- cell 保存的是开始时的对象引用，并非每次归还重验证仍挂载于网络。
- resolveSteps 不额外验证匹配列表唯一性和全套 NBT 的一致性。

## 7. 五档元件的统一注册

LoopStorageTier 枚举引用 AE StorageTier.SIZE_1K/4K/16K/64K/256K，取得 bytes、idleDrain、namePrefix。ModItems 遍历枚举注册 LoopStorageCellItem 并保留旧常量名，供材质和其他调用继续使用。

已删除 LoopStorageCell1kItem、4kItem、16kItem、64kItem、256kItem 五个只传容量的类。没有再引入 Abstract 父类；共用一个 Item 类和一个 StorageCell 实现。

容量、物品注册名、NBT 名保持不变。现有物品模型生成器仍逐条写 basicItem 五次，枚举注册并未同步重构全部 datagen 列表。

## 8. 混合存储规则

LoopStorageCellItem 管配置和持久化；LoopStorageCellInventory 管实际读写；LoopStorageCellHandler 接入 StorageCells。

- 允许的 AEKeyType 只有 items 和 fluids；精确 key 包含其附加数据。
- 配置为 key→上限数量，重复标记用累加（饱和 Long.MAX_VALUE），不是覆盖。
- canAddConfiguredAmount 对新增配置和现存内容逐 key 取较大值，再算总字节，防止已有物品超出编辑后目标时忽略占用。
- 用量按每种 key 的 ceil(amount / getAmountPerByte()) 求和；没有另收 AE 普通元件的类型开销。
- addConfiguredAmount 本身不强制容量校验，调用者必须先 canAdd；这是公开接口的实际契约。
- clearConfiguredAmounts 只清配置，不清已有内容，也不清 UUID。

普通 MEStorage.insert/extract 都只允许 `host == null && source.machine 是 IOPortBlockEntity` 的元件实例，确保 IO 端口只能操作端口内元件，而不能借自己的来源身份抽走驱动器中的种子。

循环执行走 extractForLoop/insertForLoop 专用入口；普通终端、总线或合成抽取被拒绝。归还专用入口按配置上限收货，并不重新计算字节容量。

每次 MODULATE 修改立即 persist 至 ItemStack，若有 host 再 saveChanges。SIMULATE 不落盘。

## 9. 可见性、指示灯和 tooltip

LoopStorageCellVisibility 解开 AE DelegatingMEInventory，使用 identity visited 防递归环。NetworkStorage.getAvailableStacks：

- 没普通 StorageCell 时循环元件内容正常参与显示。
- 同时有循环和普通 StorageCell 时只枚举非循环库存。
- 外部接口等非 StorageCell 库存不抑制显示。
- 保留 AE mountsInUse 重入保护。

所以“别的存储”在代码中特指可识别的 StorageCell；第三方自定义包装或非元件存储不一定等价。

灯色依据真实字节：空为 EMPTY，字节满为 FULL，否则 NOT_EMPTY，不再把“已达标记数量”误报为满盘。

tooltip 使用 AE bytesUsed/typesUsed 和 StorageCellTooltipComponent，内容按数量降序、遵从 AE 显示上限。typesUsed 的上限参数采用 capacityBytes（每 key 最少一字节的理论上限），不是原版物品盘固定类型数。
