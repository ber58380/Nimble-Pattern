# 06 已实施优化与未解决的性能成本

本章描述当前存在的优化，不是新的实施计划。无整包 TPS/分配率基准数据，不给出未经测量的“提升百分比”。旧摘要见 ../optimization.md。

## 1. 优化逐项表

| 项目 | 历史问题/动机 | 当前实现 | 失效/清理与代价 |
| --- | --- | --- | --- |
| 元件枚举注册 | 五个仅区分容量的派生类重复 | LoopStorageTier 引用 AE StorageTier，ModItems EnumMap 注册统一 Item | 旧注册名/NBT 保留；新增档位仍需补语言/模型/材质/创造栏 |
| 解码元数据缓存 | hash/equals/dispatch 重复复制 ItemStack、读 NBT | PatternMetadata、definition/identity/identityHash 构造时计算 | 包装实例不可变；原对象原地修改需重建 |
| 原始身份显式映射 | 包装与原样板 equals 不对称，重载/总成查找失败 | ProviderPatternIndex compiled→native | 重建覆盖、令牌卸载、弱键、server stop clear |
| 原版供应器编译缓存 | 重复 getAvailablePatterns 分配包装列表 | 比较对象引用序列+fuzzy 卡状态 | 检查仍 O(P)，但命中无重新解码；不检测原地变异 |
| 概率恢复索引 | 每张对外样板都扫描整柜实体样板，最坏重复解码 | 一次 Index 扫描，主产物 GenericStack 分桶 | ProviderState 重建失效；同桶多候选仍线性 |
| 普通供应器快速路径 | 无概率样板仍解码所有处理样板 | 先扫描是否有 probability，否即停止解码 | 仍有一次轻量库存/NBT 判断 |
| 控制器懒加载 | 普通 CPU 任务分配工具/循环/概率状态 | 工具 tools()、循环 start、概率 accessor 按需建立 | finish 清理；小的 FuzzyOutputLedger 仍常驻 |
| 统一特殊收货入口 | 多套 HEAD 回调争抢同一输入 | 工具→循环→概率补料，命中即返回 | 有序但耦合，需要组合回归 |
| 模糊账本索引 | 过期模糊等待造成重复结算/错计量 | dropSecondary 分桶，精确扣账同步删索引，实际 waiting 为准 | NBT 保存；完成/取消清空 |
| 概率 byOutput | 每次来货遍历全部 Attempt | key→候选 Attempt 列表 | 收满移除，批次移除/载入重建 |
| 同 key 补单合并 | 多个失败批次各启动一次规划 Future | retryDebts 保留归属，一次请求聚合缺额 | 每 CPU 同时一个 Future；没有跨 CPU 合并 |
| 轮询局部查询复用 | 同 tick 多批次反复查机器/pending | 每轮 machines、activeOutputs、pending 临时缓存 | 每 10 tick 新建，避免长期缓存陈旧状态 |
| 反射方法缓存 | 重复 getMethod | ProbabilityRecipes ClassValue+ConcurrentMap；GtlBatchAdapter ClassValue Optional；KNOWN_LOGIC | 前者仅缓存成功方法，缺失方法仍会重试；字段查询未统一缓存 |
| 无票据完成事件快速返回 | 所有 GT 配方完成都反射原/实际配方 | hasTickets=false 立即返回 | 不等同于去掉 Mixin 调用开销 |
| 票据主动释放 | 弱引用等待 GC 期间仍有无效列表 | 完成/取消/移除 attempt 时 unwatch | 不同 job 的其他票据保留 |
| 活跃概率 CPU 集合 | 每次网络插入扫描全部 CPU 找优先任务 | ACTIVE 弱集合与网络 CPU 集合求交 | 无活跃任务走原 AE；active 直到 job 结束，遍历顺序不保证公平 |
| GTL 概率批数传递 | 强行单次降低吞吐，倍率重复/漏计 | getOperations→ThreadLocal→dispatched，次数和欠账各乘一次 | 工具仍单次；乘法提前限幅；非 GTL 默认 1 |
| 全服更新条件索引 | 由最后打开的终端替换追踪条件、反复全量标签处理 | 服务器每 20 tick 遍历活动网络，按版本更新 condition→slots | 保留全服触发语义；通知增加最多约 1 秒延迟 |
| ID 去重 | 高频输入每次都分配/重复触发同 ID | pendingIds LinkedHashSet<ResourceLocation> | 每轮消费，暂不匹配也保留到刷新 |
| 库存共享快照 | 多人终端各自反复扫描相同库存 | WeakHashMap<InternalInventory,Snapshot>，默认 5 tick 复用 | force=true 绕过节流；比较仍 O(槽数)，只复制变化项 |
| 服务端终端节流 | 每 tick 枚举网络+排序条件 | 每 5 tick 检查，revision gating，引用计数+conditionsDirty | 最新显示可能延后约 0.25 秒；多人仍各维护 client diff |
| 客户端增量合并 | 每个网络包立即筛选排序 | viewDirty、fullUpdateInProgress | 一帧一次重建；搜索变化仍立即遍历 |
| 可见槽复用 | 每帧创建整页 Slot | slotsDirty/lastSlotScroll/lastSlotRows | 滚动、布局或数据改变才重建 |
| 停服清理 | 静态对象持有上一存档对象 | NimblePattern.onServerStopped 清更新索引、供应器映射、概率 ACTIVE、机器票据 | ClassValue 随类生命周期；客户端搜索缓存不在此列 |

P 为某供应器样板数。这里的“历史问题”来自当前 diff、旧实现替换和已有修复记录，不代表全部优化都能独立测出收益。

## 2. 复杂度边界

### 供应器恢复

此前反复为每个样板扫描库存容易产生 O(P²) 次库存访问。现实现对每次 ProviderState 重建做一次概率实体库存扫描和候选解码，再按主产物桶查找。不同产物分散时收益较大；所有样板同主产物时桶内比较仍可能退化为二次规模。

循环标签恢复仍采用候选列表线性查找，不是相同程度的索引优化。循环标签采集和概率 Index 也分别访问库存，不应宣称整个 ProviderState 永远只扫描一次。

### 终端与更新器

稳定库存每个共享周期最多一次默认 snapshot 扫描；修改后每个菜单仍比较其已发副本。全服更新器每 20 tick 使用 force=true，因此“最多每五 tick 一次”只适用于非强制调用，不能当全局硬保证。

客户端数据变化时仍 O(P) 搜索+O(M log M) 排序，M 为匹配数量；优化减少频率，不改变算法阶数。按钮绘制仍多次创建 Blitter，也不在本轮优化范围内。

### 网络与 CPU

无概率任务不进入优先分发集合分配；有活跃任务时每次 insert 创建 selected 快照，避免处理过程中修改 ACTIVE 导致迭代错误。ACTIVE 很大、多个网格频繁插入时仍需测量。

LoopStorageCellVisibility.unwrapCell 每次建立 identity visited 集合，NetworkStorageMixin 每次列表请求枚举所有挂载库存；仍有优化空间，不能缓存到忽略挂载变化。getConfiguredAmount 每次读取整份配置 NBT，同样未缓存。

## 3. 优化同时修正的正确性

- 精确收货同步清掉模糊欠账，防重复计数。
- 物理产出留 CPU，最终请求量只用于结账，防循环净产出被扣两遍。
- 更新/source 注解从执行身份剔除，避免管理员更新条件后正在运行的任务失配。
- 卸载带注册令牌，避免旧 ProviderState 清掉新映射。
- 概率恢复将普通重复配方纳入歧义判断，不能因为想优化而误恢复标签。
- 批数从真正 getOperations 传递，避免靠空输入或已经放大的输入反推。
- 取消完整清理循环/概率 transient 状态，避免补种后控制器残留 busy。

## 4. 性能验证与下一步测量

现有 OptimizationGameTests 有 512 槽、512 次恢复查询库存读取计数，以及 20 个读者共享快照的计数断言。它们证明“没有重复扫描”的特定性质，不证明端到端毫秒耗时。

建议以固定 MC/AE/GTL/Java/模组配置测：

1. 无特殊样板的普通网络作为负开销基线。
2. 1k/10k/更大样板库，各自产物唯一与大量同产物两种分布。
3. 1、10、50 个终端同时打开；每秒不同程度槽变化。
4. 1、16、64 个 CPU，普通、工具、概率、循环混合。
5. 每 tick 入网事件、概率 Attempt 数、活跃 CPU 数、机器票据长度。
6. JFR/采样剖析中的 tick p50/p95/p99、allocation rate、GC、主线程调用热点。
7. 多次暖机和等负载对比，再给出收益结论。

禁止只比较一次 runClient 启动时间或把 GameTest 总时长当模组性能。保留检测语义和物料守恒优先于省略验证；风险项不得因追求吞吐被静默放宽。
