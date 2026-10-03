# 07 Mixin、网络协议与持久化契约

## 1. Mixin 加载条件

主配置 nimble_pattern.mixins.json：required=true、JAVA_17、defaultRequire=1。未指定 priority 的通常使用 Mixin 默认值；表中数字只记录源码显式设定，不把数字本身当成所有其他模组覆盖一定兼容的证明。

ExtendedAE 独立配置 required=false、defaultRequire=0，目标 PartExPatternProvider。GT/JEI 的可选注入部分使用 @Pseudo/require=0；兼容性失败可能静默降级，而不是启动时必然报错。

## 2. 注入表

路径前缀为 mixin/ae2/；单纯 accessor 也属于对 AE 私有布局的依赖。

| 文件 | 目标与入口 | 改动/契约 |
| --- | --- | --- |
| CraftingCpuLogicMixin | CraftingCpuLogic，priority=1200；trySubmitJob RETURN、tick HEAD、executeCrafting HEAD/内部调用、insert HEAD/内部调用、NBT TAIL、finish/cancel HEAD | 预留工具、活动循环接管、记录派发；重定向 provider.pushPattern、waiting insert、link insert、waiting extract、final matches；特殊收货及守恒核心 |
| ProbabilityCpuMixin | 同 CPU，priority=1300；tick/finish HEAD、waiting RETURN/TAIL、NBT TAIL | 懒加载概率控制器、轮询、补充等待、保存/清理 |
| PatternProviderLogicMixin | PatternProviderLogic，priority=1100 | 升级库存、NBT/drop/clear、编译包装缓存、contains 兼容、模糊解锁 |
| NetworkCraftingProviderStateMixin | NetworkCraftingProviders$ProviderState 构造内 getAvailablePatterns redirect、unmount TAIL | 所有供应器统一包装、实体标签恢复、native 映射生命周期 |
| CraftingTreeProcessMixin | CraftingTreeProcess.request HEAD | 检查循环保留种子，正常分支失败/模拟缺量报告 |
| NetworkCraftingSimulationStateMixin | NetworkCraftingSimulationState 构造 TAIL | 普通计划库存排除循环元件 |
| ToolCraftingPlanMixin | CraftingSimulationState.buildCraftingPlan RETURN | 对最终计划一次性增加工具预留 |
| ToolJobReloadMixin | ExecutingCraftingJob NBT 构造内 decodePattern redirect | 恢复所有包装模式、装配室接口、execution_fuzzy |
| ProbabilityEncodingMenuMixin | PatternEncodingTermMenu.encode 内 encodePattern redirect；实现 Menu 接口 | 处理编码切换；服务端归一化由接口调用 |
| ProbabilityJeiTransferMixin | 可选 JEI EncodePatternTransferHandler.transferRecipe RETURN；client 配置 | 成功 transfer 后传 recipeId，包含清除旧来源的 unknown 哨兵 |
| ProbabilityProviderTargetMixin | PatternProviderLogic，priority=1200；push HEAD、sendStacksOut 前、ICraftingMachine.of redirect | 清旧目标、记录真正投料方向/候选；部分 require=0 |
| ProbabilityGtCompletionMixin | 可选 GT RecipeLogic.onRecipeFinish HEAD/TAIL，priority=1500，require=0 | 捕获原配方 ID 和 parallels，按票据完成次数记账 |
| ProbabilityJobMixin | ExecutingCraftingJob 字段 shadow + 新接口方法 | 合并 tasks；访问 waitingFor；反射增加时间统计工作量 |
| ProbabilityPriorityMixin | CraftingService.insertIntoCpus HEAD，priority=1300，可取消 | 概率 CPU 优先，剩余给普通 CPU |
| GtlLoopBatchMixin | 可选 CraftingPatternAutoExpand.getOperations HEAD/RETURN/调用 redirect，require=0 | 工具串行；原始身份询问批数上限；概率限幅；批数 ThreadLocal |
| NetworkStorageMixin | NetworkStorage.insert RETURN、getAvailableStacks HEAD | 更新事件；混合元件列表隐藏；保留 mountsInUse |
| EncodedPatternItemMixin | EncodedPatternItem.appendHoverText TAIL | 概率、工具、更新条件/状态 tooltip；注意它没有循环专用 tooltip 段 |
| PatternProviderBlockEntityMixin | PatternProviderBlockEntity.logic | 实现 IUpgradeableObject，支持卡片交互 |
| PatternProviderPartMixin | PatternProviderPart.logic | 同上，部件版本 |
| PatternProviderMenuMixin | PatternProviderMenu 构造 TAIL | 添加升级槽，放 slots 前面并重编号 |
| PatternProviderScreenMixin | PatternProviderScreen 构造 TAIL，client 配置 | 增加 UpgradesPanel 及兼容卡提示 |
| CraftingCalculationAccessorMixin | CraftingCalculation.networkInv/simRequester/addMissing | 工具快照读取；循环种子检查与缺料报告 |
| NetworkCraftingSnapshotAccessor | NetworkCraftingSimulationState.list | 读规划快照 |
| ExecutingCraftingJobAccessorMixin | ExecutingCraftingJob.remainingAmount/playerId | 最终目标结账上限、取消通知接收人 |
| NetworkStorageAccessorMixin | NetworkStorage.priorityInventory | 查循环元件和排除种子的库存枚举 |
| DelegatingMEInventoryMixin | DelegatingMEInventory.delegate | 解开挂载包装 |
| ../extendedae/PartExPatternProviderMixin | ExtendedAE PartExPatternProvider.logic | 提供升级能力，方块版本由继承原版目标获得 |

重点复查重定向在 GTLCore/MAE2 overwrite 后究竟命中几次、目标参数和返回值是否一致。require=0 不应代替兼容诊断；GameTest 加载某 Mixin 不证明整包中仍命中预期目标。

## 3. 通信协议

SimpleChannel 名 nimble_pattern:main，版本字符串 "2"，两端严格相等。

| ID | 包 | 预期方向 / 注册强制方向 | 载荷与处理 |
| --- | --- | --- | --- |
| 0 | ClearPacket | S2C / 未显式限制 | 无载荷；当前终端 clear，进入全量批次 |
| 1 | PatternPacket | S2C / 未显式限制 | inventoryId、full 标志、可选 size、slot→ItemStack；全量/增量更新 |
| 2 | ConditionPacket | S2C / 未显式限制 | 条件字符串集合；客户端历史补全 |
| 3 | PatternUpgradeNotificationPacket | S2C / 未显式限制 | condition、count；客户端 toast |
| 4 | PatternSyncCompletePacket | S2C / 未显式限制 | 结束全量同步，集中排序重建 |
| 5 | LoopSeedLostNotificationPacket | S2C / 未显式限制 | AEKey；种子丢失 toast |
| 6 | ProbabilityRecipePacket | C2S / PLAY_TO_SERVER | containerId、recipeId；服务端菜单匹配后解析自己的配方 |
| 7 | ProbabilityFailurePacket | S2C / PLAY_TO_CLIENT | AEKey；概率失败 toast |

所有 handler 通过 enqueueWork 进入主线程。5、3、7 的客户端展示使用 DistExecutor/内部客户端类；旧 0、1、2、4 handler 直接引用 Minecraft 客户端类。

严格审核不可忽略：旧包缺显式方向，计数/槽位读取也没有统一上界校验；客户端状态包缺 menuId，旧终端迟到包可能落到新开的同类型 Screen。源码中并非这些边界都已修复。AE 原 InventoryActionPacket 和 Menu client actions 不在此通道 ID 表内。

## 4. 样板 NBT

根路径 nimble_pattern：

| 字段 | 类型/语义 | 生命周期 |
| --- | --- | --- |
| source | compound: dim 字符串、pos long、side byte、slot int | 从供应器取出时写，成功回填去掉 |
| update | compound: condition 字符串、status byte ordinal | 应用 LATEST，事件 UPDATE，清除删除 |
| loop | compound，见下 | 每次成功标记替换；写入时去 probability/tools |
| tools | GenericStack compound 列表 | 成功标记替换；写入时去 loop |
| probability | compound: recipe 字符串、cycles long | 编码时标记；相同内容再次编码返回无该标签的 AE 新栈 |
| execution_fuzzy | boolean | 包装 definition 中记运行模式；重载先剥离再解码恢复 |

loop 内：group UUID、cell UUID、index int、size int、entry/exit GenericStack、seed long、netOutput long、externalInputs/mainOutputs 的 GenericStack 列表。

风险：getLoopData 只做部分读取保护，不完整验证 index/size/list 长度、净量正数等；ToolPatternData.read 只过滤非物品和非正量，没有统一版本号/迁移器。不得把“read 没抛异常”视为完整合法性证明。

## 5. 元件与终端 NBT

元件 ItemStack 顶层：

- LoopStorageConfig：[{Key: AEKey 通用 compound, Amount: long}]。
- LoopStorageContents：同结构；保存真实数量。
- LoopStorageCellId：UUID；绑定循环组，不是注册名。
- 读重复条目时饱和累加；只接受物品/流体键和正数量。

终端由 Part→Logic 保存：

- mode：枚举名字符串，非法退回 UPGRADE。
- inputPattern、conditionItem、loopEndpoints、loopStorageCell、tools：AppEngInternalInventory 标准编码。
- 供应器浏览列表、serverId、conditionsHistory 是临时会话状态，不写在终端库存存档内。

注册从五个子类切到枚举没有改上述键；删除 Java 类不会直接改变 Item 注册身份。旧“样板更新终端”历史名称是否存在更早版本迁移需要按对应发行版另审，本工作区没有完整跨所有旧版本迁移证明。

## 6. CPU 存档与未保存状态

| 状态 | 保存 | 恢复策略 |
| --- | --- | --- |
| 工具 | nimble_tools.active/reserved/expected/received/borrowed | load 四套账，无 job 时释放真实持有 |
| 模糊 | nimble_fuzzy GenericStack 列表 | 恢复分组；无 job 清空 |
| 概率 | nimble_probability 尝试/欠账/补料/credit/工具等 | 重建 byOutput，绑定 ACTIVE；旧机器事件改为超时 |
| 循环控制器 | **未见 save/load** | 不能声称活动循环支持完整重启恢复 |
| ProviderPatternIndex | 不保存 | 供应器 mount 重建 |
| LoopBatchDispatch | 不保存 ThreadLocal | 单次分派取出/清空 |
| 更新追踪 | 不保存索引 | 由活动网络重新构建，事件队列是内存状态 |

AE 原 job 仍保存任务，ToolJobReloadMixin 负责包装恢复；这不自动等于循环 working/stepIndex 也恢复。旧模糊任务缺索引信息不能无损迁移。

## 7. 线程与生命周期

- 终端库存改动、网络包处理、CPU 实际执行、概率 tick 以服务端线程为预期运行上下文。
- AE 合成计划通常异步；工具计划明确读 snapshot，而循环种子检查读活库存，是已列出的不一致风险。
- ProbabilityController 的 Future 结果只在 tick 里追加任务；cancel 取消尚未完成的 Future。
- ClassValue 的缓存按类隔离；ProbabilityRecipes 方法缓存用 ConcurrentMap，但其他 WeakHashMap/集合并非线程安全容器。
- 停服清理并不是线程安全措施，也不是正在进行的所有任务都支持恢复的证明。
