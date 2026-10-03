# 01 核心包装、模糊合成与假合成

路径前缀：`src/main/java/com/ber/nimblePattern/`。逐文件状态见 [09](09-file-inventory.md)。

## 1. 从处理样板专用类改为编码样板包装

旧 `pattern/NimbleProcessingPattern.java` 已删除，执行入口改用 `NimbleEncodedPattern`。它实现 `IPatternDetails`，持有原始 `pattern`，不重新实现配方引擎。

`wrap(pattern, fuzzy)` 检查原始对象是否实现 `IMolecularAssemblerSupportedPattern`：

- 是：建立 `NimbleAssemblerPattern`，保留 assemble、fillCraftingGrid、isItemValid、isSlotEnabled、getRemainingItems 等装配室能力。
- 否：建立普通 `NimbleEncodedPattern`。

因此包装覆盖范围扩大了，但不等于每种特殊模式支持所有编码样板：工具标记仍限定 `AECraftingPattern`；循环解析仍限定 `AEProcessingPattern`；概率编码只在 PROCESSING 模式进行；模糊卡在原版供应器中也只为处理样板启用。

### 构造阶段缓存

构造函数一次读取 definition 和 NBT，得到：

- `PatternMetadata(probability, recipe, cycles, tools, loop)`；
- `definition`：供任务保存、重载的编码样板键；
- `identity`：用于 equals/hashCode 的执行身份；
- `identityHash`：预计算哈希；
- 输入数组、输出数组、fakeMode、fuzzyMode、有效 loopData。

存在 `execution_fuzzy` 时恢复模糊模式；传入恢复得到的 loopData 时将其补入定义副本。只接受 size>=2 的循环运行标签，旧单样板循环不进入循环执行分支。

`PatternMetadata` 是 record，工具列表及 LoopPatternData 内列表使用不可变拷贝。它用于避免在哈希比较、每次派发中反复解码 NBT，并非全局任意 ItemStack 缓存。

## 2. 三种身份必须分清

| 身份 | 保留什么 | 用途 |
| --- | --- | --- |
| 实体样板 | AE 编码 NBT、来源、更新条件、功能注解 | 玩家移动、终端管理 |
| definition | 执行所需功能注解，包括 execution_fuzzy | CPU 序列化与恢复 |
| identity | definition 去掉 nimble_pattern.source 和 update；空根移除 | Map 键、运行中任务匹配 |

只在两个 `NimbleEncodedPattern` 实例之间比较 identity；不允许包装类单方面宣称等于原始 AE 样板。这样避免对称性破坏、HashMap 查询不稳定。代价是供应器需要显式的原始身份映射。

`ProviderPatternIndex`：

1. ProviderState 构造时按相同列表索引登记 compiled → exposed/raw。
2. 若 exposed 已是包装类，登记其 getPattern()。
3. 弱键保存供应器映射，register 返回该轮 Map 作为注册令牌。
4. unmount 时仅在令牌仍等于当前 Map 时删除，避免旧状态卸载误删新状态。
5. 找不到映射时解包一次，普通原始样板直接返回。
6. 服务器停止时 clear。

注意：这是供应器重建时的快照，不是监听所有 NBT 原地修改的通用缓存。供应器若修改内部样板却不申请 AE 更新，会造成滞后。

## 3. 供应器编译路径

`PatternProviderLogicMixin` 给原版逻辑增加一格升级卡库存，实现 IUpgradeableObject。升级变化触发 saveChanges 与 ICraftingProvider.requestUpdate；write/read NBT 使用 upgrades 键，拆除掉落和清空也处理卡片。

`getAvailablePatterns` 尾部包装所有模式的样板；仅 AEProcessingPattern + 安装模糊卡时传入 fuzzy=true。缓存命中要求：

- 模糊卡布尔状态不变；
- 原始列表长度不变；
- 每个原始样板对象引用都相同。

命中复用不可变编译列表；不命中重新包装。这避免按查询次数分配包装对象，但假设原始样板对象不可原地改变其配方语义。

`NetworkCraftingProviderStateMixin` 在所有供应器进入 AE 网络时再统一处理，弥补 GTL 总成不走原版 PatternProviderLogic 的情况。它先恢复概率/循环注解，再包装未包装对象，最后登记 ProviderPatternIndex。

`pushPattern` 内 List.contains 的 redirect 容许包装类通过其原始 pattern 被供应器识别；CPU 派发则优先使用 ProviderPatternIndex.nativePattern。两者是不同层的兼容措施，不应合并为跨类型 equals。

## 4. 模糊输入

`FuzzyInput` 保留原输入的 possibleInputs、multiplier 和 remainingKey，令 isValid 返回 true，让 AE 不因 secondary/NBT 差异在此处拒绝候选。

这不是“任意物品都可以替代”：候选枚举和抽取仍由 AE 的合成流程控制。严格审核需确认目标 AE/GTL 版本的候选生成范围；isValid 本身没有再次验证物品 ID，也没有实现按耐久百分比配置的多档匹配。

供应器初始化 FUZZY_MODE=IGNORE_ALL；实际包装开关取决于模糊卡是否安装。不能将它描述为已经支持原版所有 FuzzyMode 档位。

供应器 `onStackReturnedToNetwork` 的锁定解除比较在有模糊卡时使用 dropSecondary，避免真实产物 NBT 与记录不同而一直锁定。

## 5. 模糊输出账本

`crafting/FuzzyOutputLedger` 维护 dropSecondary → exactKey → amount 候选索引，但 AE waitingFor 库存才是最终权威。

收货步骤：

1. 先对收到的精确 key 抽取 AE 等待数量。
2. 精确命中后同步减少该 key 的模糊索引；回调概率控制器记录实际已收货。
3. 精确没有命中时，查同一 dropSecondary 组。
4. 每个候选可收量是“索引数量”和“AE 实际等待数量”的较小值。
5. 实际接受量也限制为本次剩余输入量；MODULATE 才修改索引、调用收货回调。
6. 候选清空后移除组；SIMULATE 只返回预测值。
7. Receipt.fuzzy 仅在本次确有模糊收货时为 true，供最终产物匹配使用。

这修复了历史上精确产物已经结账、旧模糊索引还允许第二份变体产物结账的问题，也避免以模糊登记量替代真实欠量。

CPU writeToNBT 保存 `nimble_fuzzy`；readFromNBT 恢复，有效 job 不存在时清空；finish/cancel 清空。旧版本正在运行的模糊任务没有该索引和 execution_fuzzy 信息，无法凭空恢复，升级前应完成或取消。

## 6. 假合成的严格触发条件

`NimbleEncodedPattern.isFakePattern`：

- 不是概率样板；
- 原始样板必须是 AEProcessingPattern；
- 只有一个输出；
- 输出是 AEItemKey；
- 物品属于 BookItem，且有自定义名称。

不是任意“命名物品”，也不是合成样板输出命名书就自动变成假样板。工具包装进一步保证有效工具样板不按假样板执行。

CPU 在成功派发后记录 currentPattern。AE 在 executeCrafting 登记等待产物时，如果它是假样板且该输出匹配最终任务目标，先登记等待，再立即调用 CPU.insert。此时临时 isFakePattern 标志让 CraftingLink.insert 返回接受数，不产生真实书本。

因此“假完成”只模拟最终书本回流；输入仍真实派发。当前代码没有证明把命名书作为多级配方中间材料也能虚拟完成。不能将此实现当作通用零耗材配方引擎。标志用 try/finally 清理；不能以过期 currentPattern 判定任意后续收货都是假合成。

## 7. CPU 共用收货顺序与守恒

CraftingCpuLogicMixin 的 insert HEAD 先处理工具返回，再处理循环当前步骤，最后才是概率缺料收集。如果工具/循环已接管，就终止本次普通插入路径。

普通/模糊产物通过 AE 等待账本结算；概率 received 监听的是实际扣掉的等待量。工具与循环的最终产物可能先放入 CPU 真实库存，再走一次特殊结账：

- standalone 任务：确认已结账，真实库存交给 finishJob 回网。
- 有 requester：保留 CraftingLink 交付，只有 requester 实际接受的部分才从 CPU 库存扣除。
- 概率 standalone：将实际收到的最终物品归 CPU 所有，避免同一物品被两个 CPU 重复计为已交付。

审核重点是每条分支中“物品插入一次、扣减一次、欠账减少一次”，尤其混合工具/概率/循环任务和同步返回供应器。
