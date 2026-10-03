# 08 测试证据、审核风险与验收矩阵

## 1. 已有证据与本轮范围

本轮只写文档，没有重新执行游戏测试，也没有修改业务源码。查验现有 optimization-test.log 得到：

- Running test batch (32 tests)；
- All 32 required tests passed；
- BUILD SUCCESSFUL in 48s。

这是此前优化轮的测试记录。上一轮图标接入后另有 build 成功和 jar 含两张 PNG/TextureTabButton 的确认；未进入游戏验收图标。文档检查不能替代重新构建当前待发布版本。

现有日志被 .gitignore 忽略，不保证随源码交付；复审应重新运行并保存自己的输出与版本。

## 2. 可复现命令

在项目根目录、Java 17 环境：

```powershell
.\gradlew.bat build runGameTestServer
.\gradlew.bat runData
git diff --check
```

runData 会重写生成资源，审核时应在隔离工作区执行，观察有无未提交差异。build.gradle 的 GameTestServer 使用 run-gametest 独立目录、加入 test source，并复制 empty.snbt。检测任务名含 gametest 时不加载客户端专属 JECharacters。

LoopBatchSizingTest 是普通 main，不是 JUnit 或 @GameTest；`gradlew test` 和 32 项 GameTest 不意味着它自动执行。编译后可单独：

```powershell
java -cp "build/classes/java/main;build/classes/java/test" com.ber.nimblePattern.crafting.LoopBatchSizingTest
```

本轮未执行以上运行命令。不要仅以 build/test 成功宣称所有 Minecraft 游戏测试通过。

## 3. 自动测试方法清单

### ToolPatternGameTests（6）

| 方法 | 核心覆盖 |
| --- | --- |
| unbreakableToolWaitsForRemainder | 无限耐久工具等待真实余物，不提前结束 |
| templatePlanReservesOneTool | 模板净增产，任务一次工具预留 |
| templateHundredCraftsAndReload | 多轮复用和控制器保存/恢复 |
| rejectsConsumedMaterialsAndProcessing | 消耗型材料/处理样板拒绝 |
| cpuMixinAndJobReloadLoad | CPU 注入及任务包装恢复 |
| toolCancellationAndRejectedDispatch | 取消释放、供应器拒绝后的回滚 |

### ProbabilityGameTests（13）

| 方法 | 核心覆盖 |
| --- | --- |
| editedProbabilityOutputsKeepTheirIdentity | 玩家改输出量不丢概率身份 |
| deletingExternalCatalystPreservesExecutionCount | 删除外供催化物，保留输入推导 cycles |
| expectedOutputAndFractionalScaling | 期望与小于 1 的整数化 |
| rejectUnknownOrEditedRecipe | 初次不明确映射拒绝；非后续编辑规则 |
| multipleOutputsUseCommonDenominator | 多产物共同分母 |
| completionTicketsDoNotDoubleCount | 共享机器事件不能重复归属 |
| tagRemovalKeepsExpectedQuantities | 取消标记保留数量 |
| retryLedgerPersistsAndCancelClears | 补料/credit 保存和取消 |
| probabilityMixinsLoad | 注入能在测试运行时加载 |
| failedRollWaitsForMaterialsThenCompletesOnSameCpu | 实际 AE CPU、测试库存/供应器，失败缺料补料完成 |
| failedBatchesCoalesceIntoOneRetry | 同 key 多批次合并补单 |
| gregtechAdapterReadsActualContentApi | 当前开发 GT API 概率字段适配；非完整 GTL 验收 |
| reconstructedProviderPatternSurvivesJobReload | 实体概率标签恢复和运行身份重载 |

### OptimizationGameTests（13）

| 方法 | 核心覆盖 |
| --- | --- |
| batchLimitsUseProviderNativeIdentity | 批次上限询问使用供应器原始身份 |
| fakeModeIsCachedAndCannotOverrideProbability | 命名书判定和概率优先保护 |
| updateConditionsAreTrackedAcrossNetworksWithoutMenus | 未开终端的多网络条件追踪 |
| providerIndexAndMenuSnapshotsAvoidRepeatedInventoryScans | 512 槽索引与 20 读者快照读取次数、原地 NBT 变化检测 |
| exactReceiptsCannotLeaveStaleFuzzyDebt | 精确来货清除模糊残债 |
| fuzzyReceiptsUseActualDebtAndSurviveReload | 真欠量上限、模拟无写、持久化 |
| patternIdentityIsSymmetricAndKeepsFuzzyMode | 相等性对称、模糊身份、管理标签不改变执行 |
| providerMatchingUsesNativePatternAfterReload | 重载 wrapper 到供应器 raw 身份映射 |
| providerUnmountCannotDeleteReplacementIndex | 注册令牌保护新映射 |
| enumCellsKeepCapacityIdsAndCumulativeConfiguration | 五档枚举容量/ID/耗电、累加配置 |
| probabilityIndexDoesNotGuessAmongOrdinaryDuplicates | 普通重复候选导致歧义，不错误恢复 |
| probabilityBatchCountsEveryOutputAndCleansPriority | 批次数量与欠账、保存、活跃优先状态清理 |
| completedMachineTicketsAreActivelyRemoved | 完成/注销立即释放票据 |

独立 LoopBatchSizingTest 覆盖 1/2/4/3 增殖、足种子直接大批、净增 8 的边界、0 种子、Long.MAX_VALUE 等纯算术场景，不含机器/元件/CPU 的完整闭环实机测试。

## 4. 需要优先审核的风险

以下是**源码观察/风险推断，不是全部已经在游戏中复现的 bug**。本次不修改这些实现；开发者需判断严重性、复现并单独修复。

| ID | 代码依据 | 可能后果与建议验证 |
| --- | --- | --- |
| R01 高 | LoopCraftingController 没有 save/load；CPU NBT hooks 只处理 tools/fuzzy/probability | 在环中途存档重启可能丢失控制器 working/步骤/种子归属。必须单独设计循环持久化或明确不支持 |
| R02 高 | CraftingTreeProcessMixin.request 通过 live grid 和 LoopStorageCellAccess.find 查询库存；相关 Map 非并发 | 异步计划与主线程移除/补种竞争；与工具 snapshot 策略不一致。并发下单+热插拔测试 |
| R03 高 | loop acceptOutput 末步归还失败时 active 保留，但 stepIndex>=steps.size 导致 tick 返回 | 可能永久等待且无恢复入口；测归还时元件移除、配置修改、满额竞争 |
| R04 高 | NimblePatternTag.getSource/pushPatternBack 按坐标回填，缺少明确安全权限/同网验证；slot 负值未显式拒绝 | 人工 NBT、跨网/跨玩家来源是否可非授权写库存，需从菜单入口到目标库存整体鉴权审查 |
| R05 高 | LDlib 库存反射适配器 setItemDirect 吞异常，pushPatternBack 随后返回 true | 第三方写失败时终端可能清空手持样板，需故障注入验证并改为可确认写入 |
| R06 高 | Protocol ID 0..5 未显式限定方向；部分 handle 直接调用客户端类；集合长度/槽位上界较少 | 恶意/错误包导致服务端加载客户端类、越界/资源放大。增加方向、长度、menu 会话校验 |
| R07 中高 | ProbabilityJobMixin 反射私有 TaskProgress/value/addMaxItems；GT 部分 require=0 | 升级 AE/GTL 后追加任务失败或无完成事件。启动兼容自检/集成测试优于静默失败 |
| R08 中高 | ProbabilityController.dispatched 在 provider.pushPattern 返回后才注册 Attempt | 同步供应器可能在记录前已回货；当前 AE 欠账/credit 顺序需专门构造同步回流测试 |
| R09 中高 | 循环元数据恢复仅按主产物 key+amount，候选唯一性仅在已标记候选间；概率恢复额外检查普通重复候选 | 同产物普通/循环配方混放时循环标签是否被误恢复，需构造歧义柜测试 |
| R10 中 | Tool/loop/probability 根标签没有统一模式和版本校验；getLoopData 只部分过滤 | 手改/老 NBT 可触发列表越界、负倍率、异常组合。增加结构验证和版本迁移 |
| R11 中 | 已知机器不超时，cycles 依赖完成事件；resolveProvider 只在 idle 时采用追踪 | 某版本完成 hook 不命中或并行计数语义不同会卡住；忙机转超时也可能提前补单 |
| R12 中 | 概率无总重试预算，warned 控制整个控制器生命周期 | 极低概率/玩家高报产出无限消耗，后续缺料可能不再提示。明确策略并提供诊断指标 |
| R13 中 | LoopStorageCellAccess/Visibility 只识别特定 AE 包装，find 绑定 UUID；元件 item 的配置 API 可绕过 canAdd | 第三方包装、复制 UUID、外部调用越容量、共享种子并发需检验 |
| R14 中 | 所有循环共享单个 CPU 控制器；若 loop active，executeCrafting 整体由其 tick 返回 | 同 CPU 普通独立步骤可能被串行阻塞。先保正确性再测吞吐 |
| R15 中 | 原版编译缓存靠对象引用；客户端 tag/tooltip 缓存没有统一资源重载钩子 | 原地配方修改/语言或 tag reload 陈旧，需明确失效契约 |
| R16 中 | 回填和应用是多次真实库存写入，非跨供应器事务 | 中途第三方异常可能部分提交；“先验证”仅减少正常失败，不等价事务 |
| R17 中 | src 实际 NimblePattern.java 与 Git 已跟踪 Nimblepattern.java 大小写差异 | Windows 构建通过不代表 Linux 新 checkout 通过；提交前核对文件名与 public class |
| R18 低/界面 | tabs PNG 为大分辨率生成图，非 16 像素原生；最新图标未实机检查 | GPU 缩放细节/资源包对比与打包体积需验收 |
| R19 语义 | 更新条件全服触发，无线终端通知不核对目标网格 | 不是私人网格隔离功能；如需隔离必须改设计而非仅改文案 |
| R20 语义 | fake 完成只处理最终输出匹配；循环按唯一线性链和精确数量 | 不应宣称任意假中间物或任意复杂环已支持 |
| R21 中 | ToolCraftingPlan 在全计划末尾追加缺工具，但多路径选择可能已完成 | 存在其他可行非工具配方时是否会错误报告缺工具，需多配方分支计划测试 |
| R22 中 | ToolCraftingController 的 false 回滚假设 provider 未消费也未回货 | 非标准供应器先回货再 false 等异常契约需显式拒绝或事务保护 |

## 5. 发布前手工矩阵

每组记录模组精确版本、配置、样板 NBT、初始库存、CPU 状态与最终所有库存的总量。

1. 模糊：同 ID 不同 NBT 作为输入/产物；精确与模糊混合来货；两个 CPU 同时等同 key；重载、撤卡、改更新条件。
2. 假：命名书最终任务完成但不生成书；非命名书/多输出/概率命名书不误触发；作为中间产物的限制。
3. 终端：三页切换记忆、81 槽滚动、缩放、多人同开、虚拟流体、未知来源回填失败保留、满目标槽、不加载区块。
4. 更新：两张网均未开终端、条件到货去重、任意文字不自动触发、普通入网与 SIMULATE 区别、通知携带无线终端的条件。
5. 循环：A→B、B+D→C、C→2A；GTL 芯片例子，分别请求 A/B/下游物 1、10、100；记账与真实余量必须一致。
6. 循环故障：每步取消、种子已转化、IO 补种后重下单、元件取走/拆除/区块卸载/重启、多 CPU 争同元件。
7. 存储：五档容量、混合流体/物品、重复标记、普通入口拒绝、IO 两向、驱动器不可借 IO 来源越权、普通元件出现/消失后的可见性。
8. 工具：无限耐久真实返回、不返回拒绝、模板 100 次、两个配方共享工具、同 key 普通消耗+工具、拒绝投料、取消、重启。
9. 概率：JEI+基础概率、删蜂群、改产物数量、二次编码、换确定配方、关闭重开、未知配方。
10. 概率运行：单机、多方块输入仓、超级样板总成、共享仓、复杂子网、Create 序列；缺电/暂停/机器拆除/区块卸载/重启。
11. 概率补料：0/部分/足量成功，迟到货、同产物多批合并、玩家补足材料、供应器被拆、多个概率 CPU、公平性与取消。
12. 兼容：最小 AE 环境、开发 GT、真实 GTL/GTLAdditions/ExtendedAE/MAE2 组合分别验 Mixin 日志；不能互相替代。

不自动复制任何 jar 到整合包。需要审核者主动将确认版本装入测试副本，并保留存档备份。
