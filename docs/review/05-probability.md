# 05 概率样板

## 1. 识别不是扫描所有配方

入口为 AE JEI EncodePatternTransferHandler.transferRecipe 的成功 RETURN。只有真正 transfer 且无错误才发 ProbabilityRecipePacket(containerId, recipeId)，在 AE 原填槽消息之后提交身份。

客户端不上传成功率和结果数量；服务端确认 sender、当前 menu/containerId 和 ProbabilityEncoding.Menu，再由服务端 RecipeManager.byKey 取配方。只在样板编码终端 PROCESSING 模式解析。

手工录入没有 recipeId，不全库猜测；不支持的配方保持普通样板。因此这里不是“所有模组概率配方通用识别器”，是统一协议下的明确适配器集合。

## 2. 原始配方适配范围

ProbabilityRecipes.unwrap 识别 Recipe<?> 或 GTRecipeWrapper 的 recipe 字段。outputs 根据具体类/父类识别：

| 来源 | 支持内容 | 拒绝/降级 |
| --- | --- | --- |
| GTRecipe | 普通物品/流体输出，ChanceLogic.OR 或 GTL LongChanceLogic.OR | tickOutputs 非空、非 OR、无法唯一转换的产物 |
| Create ProcessingRecipe | getRollableResults，流体结果按必定产出 | 未知 API/异常内容 |
| Create SequencedAssemblyRecipe | resultPool，每项权重除总权重 | 无法读取或非法分母 |
| 其他 | 无 | 返回空列表，不猜概率 |

GT 每个 content 使用 chance/maxChance，chance 限制到 [0,max]；max<=0 拒绝。不固定分母 10000。不读取 tierChanceBoost，不将高电压、超频、机器修正后的配方用作编码期望。

Create 序列装配读取最终池权重，不把装配循环次数重复乘入最终成功率。此适配依赖反射字段/API，并非实际 Create 测试通过的保证。

输入的概率消耗不参与“是否概率样板”的判定；初次期望归一化仍按 JEI 所填全部输入完整消耗处理。

## 3. 精确期望与整数化

ExpectedAmount 使用 BigInteger 分子/分母并约分；小数经 BigDecimal.toString 表示转换，不用 double 连乘后随意四舍五入。

normalize：

1. 按输出 key 累加原始最大数量，以及 quantity×chance 的期望。
2. 只取 JEI 实际显示的输出，不主动增加 JEI 隐藏的副产物。
3. 初次显示量必须和原配方同 key 的原始输出量相同，否则无法确认映射，返回 null。
4. 必须含真正低于必定产出的选中输出，且期望>0。
5. 取各选中期望分母的最小公倍数 factor。
6. 全部输入乘 factor；输出变成 expectation×factor；cycles=factor。
7. 任何整数溢出、数量超过编辑器 Integer.MAX_VALUE 等情况拒绝，不截断。

例：A+B→50%×2C，得到 A+B→1C、cycles=1；A→10%×1C，得到 10A→1C、cycles=10。

不是固定“翻倍一次”，是求足够的共同整数倍率；流体内部数量也按此整数量纲处理。

## 4. 删除催化输入与手调产出

ProbabilityEncodingMenuMixin 保存当前导入的 recipeId、Normalized、原始输出列表。初次只 normalize 一次并回写编辑器；后续点击编码不再次强改玩家数量。

hasProbabilityOutput 只检查：

- 当前显示输出非空、数量>0；
- 所有输出 key 都属于所选配方；
- 至少一个 key 有非零且小于 1 的概率来源。

因此删除龙纳米蜂群等外供输入不取消标记；把期望 1C 手调为 2C 也可以编码概率样板。换无关产物或只留下必定产物则不标记。

editedCycles 根据仍保留且可对照原输入的数量比例计算执行次数，不从玩家修改的输出量反推。比例非整数、不同输入推导不一致或没有可判断输入时 cycles=0，表示无法按机器完成次数准确归属，运行走超时。

局限：该来源状态保存在当前 Menu 内存中；手填配方不识别，关闭重开后不能假设仍保留上次 JEI 来源。hasProbabilityOutput 不证明玩家改后的输入能够实际完成配方。

## 5. 再次编码回退

encode 的 encodePattern 调用被重定向。先让 AE 生成当前数量的普通样板；如果输出槽 previous 带概率标签，且 previous/新样板 sparseInputs 与 sparseOutputs 完全一致，直接返回普通编码结果，不写 probability。

所以只取消标记，不恢复原始最大产量、不恢复删去的输入。判断条件是当前输出槽中的上一张和当前编辑内容相同，不是“累计点击第二次”的计数器。

首次/非回退编码，若来源与当前概率输出仍成立，写 nimble_pattern.probability{recipe,cycles}。tooltip 由 EncodedPatternItemMixin 增加“概率样板”。

## 6. 第三方重编码恢复

ProbabilityPattern.Index 每次供应器重建时扫描真实 PatternContainer 库存一次：

- 没有概率标签，立即跳过全柜 AEProcessingPattern 解码。
- 有概率标签，则也索引普通样板，按完整主产物 GenericStack 分桶。
- 对外样板输入/输出键数量必须为某实体配方的匹配子集，允许第三方删去部分输入/副产物。
- 同桶多个候选、普通重复配方、缺 recipeId 均不恢复；不猜来源。
- 唯一匹配时，用对外原始 definition 补概率注解，而不是覆盖第三方已调整的配方内容。

该处理不是绝对识别证明；同主产物大量配方仍需线性比对候选，第三方不实现 PatternContainer 也无法靠实体库存补回标签。

## 7. 成功投料后建立 Attempt

CPU 仅在 provider.pushPattern 返回成功后调用 dispatched。Attempt 记录：

- 按 operations 放大的输出欠账 remaining；
- 原配方 ID、cycles×operations；
- 机器位置/logic（若可靠解析）；
- elapsed、grace、failed。

GTL 操作次数由 LoopBatchDispatch 传递，输出和机器次数各放大一次；GtlLoopBatchMixin 在溢出前约束 operations。不从已放大的 holders 再推一次倍率。

同一个 CPU 的 attempts 按输出 key 建 byOutput 索引。received 仅按实际 AE 等待扣减分摊给对应 Attempt；收足某 key 便从候选移除。

## 8. 机器识别与完成票据

普通 AE 供应器通过 ProbabilityProviderTargetMixin 记录 pushPattern 实际尝试/选定目标位置（sendDirection 或 ICraftingMachine.of）。总成/仓室尝试直接取得 getRecipeLogic，或沿唯一 getControllers 追到控制器。

可追踪要求：

- logic 实现由 Mixin 注入的 Tracked；
- onRecipeFinish 的声明类是受支持 GT RecipeLogic；
- 投料后解析时 isIdle 为 true；已有工作无法确定归属，退回未知机器；
- 多控制器输入仓、复杂子网、未知状态 API 不追踪。

RecipeLogic.onRecipeFinish 的 HEAD 捕获原始 recipeId 与实际配方 parallels，TAIL 发 completed。仅存在票据时才反射读取，避免每个普通配方完成都走反射。

票据按 machine logic 弱键组织、票据弱引用，按同 recipeId 顺序分摊完成次数。一份完成次数只能扣一遍，不能所有等待任务都各减一次。完成和取消主动 unwatch，不只依赖 GC。

这比“看机器从忙变闲”更保守：缺电/暂停不判失败；已知但卸载的机器不超时。它也意味着能解析但永远没完成事件的机器可能永久等待。

## 9. 失败判定

ProbabilityController.tick 由 CPU tick 驱动，仅每 10 tick 做一次实际工作：

- 未知机器：elapsed+=10，达到 config×20 后 failed。
- 已知机器：不强制加载；位置解析失败则注销票据、转未知机器重新计时。
- 已知 cycles 全完成：grace 每次+10，达到 20 tick 后允许判断欠量。
- 同次轮询复用同位置机器解析结果。

配置 `probabilityPatternTimeoutSeconds` 默认 120、范围 1..86400，Forge COMMON 文件 `config/nimble_pattern-common.toml`。按游戏 tick 算时间，低 TPS 下不等于真实墙钟 120 秒；CPU 不活动时不推进。

## 10. 补单仍在原 CPU 内

控制器不是请求新 CPU，也不是启动 ExportBus crafting card：

1. 只针对已 failed 且还欠货的产物。
2. 查询 AE job.waitingFor，缺量不能超过原任务真实欠量。
3. 有该产物仍在飞或已有 pendingOutputs 时先不重复补。
4. 按 key 汇总多个失败 Attempt 的欠量，记录各自 retryDebts 归属。
5. 调 beginCraftingCalculation，策略 REPORT_MISSING_ITEMS；每控制器最多一个 Future 在算。
6. 完成后重新检查仍欠量；若期间已部分到货，不直接追加过量旧计划。
7. 把计划 patternTimes 合入原 ExecutingCraftingJob.tasks。
8. 将替代的产物数量加入 credits；派发时登记输出欠账会先 consumeCredit，避免原本欠 3C 变成欠 6C。
9. usedItems/missingItems/emittedItems 加入 supplies；实际材料到货进 CPU inventory。
10. 工具通过 held/reserve 接口避免重复预留。

ProbabilityJobMixin 反射 AE 私有 TaskProgress.value 与 ElapsedTimeTracker.addMaxItems；先准备任务更新，再调整工作量、putAll。依赖具体内部布局，且并非跨所有对象的通用事务。

## 11. 缺料、优先收集及取消

supplies 有缺料时广播 ProbabilityFailurePacket 至全服，客户端使用通用 Toast 显示概率失败标题与产物名。warned 抑制同一控制器重复提醒；不是每次失败都刷广播。

有活跃概率任务时，CraftingService.insertIntoCpus 优先遍历活跃概率 CPU 与当前网络 CPU 的交集；剩余再给普通 CPU。只收 waiting/supplies 指定的键和量，不吸走所有相关物品。多个概率 CPU 之间没有明确 FIFO/公平级别，这是优先于普通任务而非全局唯一最高序。

每 10 tick 也尝试从网络抽取 supplies；收取成功才存 CPU。没有活跃概率控制器时立即走 AE 原路径。

clear 取消 Future、注销票据、清债务/credit/补料/工具需求和 ACTIVE；真实材料仍在 CPU 库存，交 AE 结束流程返还。已投料机器不可撤回。

无可追加样板计划时保留原欠货等待手工交付，并退回超时循环。补单异常日志/通知后设置 poll=-190，延迟再次重试。极低概率或玩家手动抬高期望产量可不断补单，当前无总次数/材料预算上限。

## 12. 持久化与风险边界

nimble_probability 保存 attempts、remaining、recipe、machine 位置、cycles/elapsed/grace/failed、supplies、credits、tools、warned、enabled、failureOutput。Future 与 retryDebts 不序列化；载入后从欠账重新规划。

load 不恢复旧机器逻辑对象和完成事件归属；有 machine 记录的在途批次转为未知追踪并重置 elapsed，避免等待一个重启前已发生的完成事件。

需严格审核同步回流（当前 Attempt 在 push 成功后才登记）、迟到回货与补单竞态、跨维度机器、共享同 recipeId 机器、无限供料库存，以及 mixin require=0 无声失效。测试替身不能覆盖这些场景。
