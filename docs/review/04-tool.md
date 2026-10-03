# 04 工具样板

## 1. 范围

当前仅支持 AE2 AECraftingPattern（合成样板）。虽然 NimbleAssemblerPattern 保留锻造、切石等 IMolecularAssemblerSupportedPattern 能力，ToolPatternData.validate 仍只放行 AECraftingPattern。处理样板、机器内部保留扳手等方案未实现。

“只需一个工具”实际指**一轮配方所需的一份数量**。若一次配方需要 2 个相同工具，任务预留 2 个；不随 100 次合成变成 200 个。

## 2. 终端批量校验

工具页四个 FakeSlot 选择精确 AEItemKey，去重；不接受流体。applyTools 对左侧所有样板先复制、解码、校验并暂存在 tagged Map。任一样板失败，整批不提交副本和回填。

ToolPatternData.validate 对每个所选 key：

1. 必须在原样板输入的首选键中出现。
2. needed 累加 primary.amount × multiplier，并用 exact 算术检查溢出。
3. produced 汇总样板全部同 key 输出。
4. 以下之一成立才有效：
   - 物品 NBT Unbreakable=true，且没有净增产时，每个相关输入的 remainingKey 都等于工具精确 key。
   - produced > needed，即工具同时为输入和净增产输出，如模板复制。
5. 写入真实 needed，不使用 FakeSlot 显示数量作为任务预留数量。

因此 Unbreakable 标签本身并不能让“配方实际消费掉的工具”合法；标记也不会修改工具耐久、复制实体或改变配方。

写 tools 标签前 removeLoopTag，防止同时以工具/循环两种控制器执行。没有统一通用模式互斥验证器；手工 NBT 可制造组合，需审查降级路径。

## 3. 包装向计划器暴露什么

NimbleAssemblerPattern 构造时从元数据再次 validate；坏/过期标签遇 IllegalArgumentException 或 ArithmeticException 退回普通行为。

- getInputs 排除工具 key，保留消耗性输入。
- getOutputs 从相同 key 的输出中减去需要保留的工具量。
- 若工具已通过 input.getRemainingKey 作为容器余物返还，扣除量先减去这部分，避免重复扣保留量。
- 只保留正输出。
- 所有实际组装和余物计算委托原样板，包装本身不生产物品。

例如 1 模板+耗材→2 模板，向 AE 公布耗材→1 模板，并且任务一次预留 1 模板。这同时消除工具输入造成的递归。

## 4. 计划一次预留

ToolCraftingPlan.reservations 遍历 patternTimes，按每个 key 取所有工具配方 needed 的**最大值**，不是逐配方相加；同 CPU 串行复用同一套工具。

buildCraftingPlan 返回时的 ToolCraftingPlanMixin：

1. 通过 CraftingCalculationAccessorMixin.networkInv 和 NetworkCraftingSnapshotAccessor.list 读取主线程创建的网络快照。
2. 把工具加入 usedItems。
3. 从快照已有量扣除“普通消耗 + 工具预留”，不足加入 missingItems，避免重复添加同 key 已有缺额。
4. 返回新的 CraftingPlan；增加 8×工具类型数 的 bytes 估算，保留其他计划字段。

这不是自动为缺失工具另下单生产：开始任务前需网络已有工具；也不是在异步规划线程直接改网络库存。

trySubmitJob 成功后，CPU 从自身已取得的真实库存把工具转入控制器 reserved。

## 5. 四套库存和一次步骤

| 库存 | 含义 |
| --- | --- |
| reserved | 暂存在 CPU 控制器、可借出的真实工具 |
| borrowed | 本步已借给装配室的工具归还义务 |
| expected | 本步预期实际输出和余物的欠账 |
| received | 已真实收到、暂不对 AE 发布的本步产物 |

push：

1. active 时拒绝另一工具步骤，保证同一份工具不被并行借用。
2. 合并消耗 holders，模拟确认 reserved 足量，再加入临时 available。
3. 用原始 pattern 调 CraftingCpuHelper.extractPatternInputs，得到完整物理网格、输出和余物。
4. 若抽取失败或临时材料仍有无法解释的残留，不派发。
5. 工具从 reserved 转 borrowed；把输出+余物加入 expected，先设 active=true。
6. 调供应器 nativePattern/完整物理 holders。
7. 返回 false：还工具到 reserved、清等待、取消 active；成功：清空外部 consumables holders。

先登记等待再投料，是为了处理 pushPattern 中同步回货的供应器。若第三方违反“返回 false 时不消费/不回货”的约定，回滚不能视为充分安全，需要测试。

## 6. 收货、发布和性能取舍

CPU.insert HEAD 优先调用 accept，按 expected 精确 key 接收，MODULATE 才进入 received。getWaitingFor/getAllWaitingFor 补充工具余物等待键，允许网络把工具送回 CPU。

finishStep 要等**全部输出和全部余物**收齐：

- 从 received 提取 borrowed 数量，归还 reserved。
- 其余真实内容作为可发布结果。
- 清 borrowed/received，active=false。
- CPU 先处理容器余物，再处理会结束 job 的最终结果，避免 finishJob 提前释放预留状态。

下一轮直接复用 CPU 内 reserved，不反复写入存储元件再抽出；工具仍需从装配室实际返回 CPU，因此没有省掉装配室/网络运输。

代价：每 CPU 工具步骤串行，GTL getOperations 对工具限制为 min(remaining,1)。不会为了高吞吐复制工具。多个不同工具配方共享最大预留量，也依赖此串行化保证。

## 7. 结束、取消、重载

release 将 reserved 与 received 的真实物品放回 CPU，由 AE 统一回网；borrowed 只是外借记录，不能据此凭空归还仍在装配室的工具。清 expected 并取消 active，迟到物品仍可正常进入网络。

CPU NBT nimble_tools 保存 active 和四个库存，load 恢复；没有 job 时释放已有真实库存。ToolJobReloadMixin 在任务反序列化解码时重新调用 NimbleEncodedPattern.wrap，并恢复 execution_fuzzy，保持装配室能力与身份。

## 8. 重点审核

- 工具 key 为精确 key，包括 NBT，不因模糊卡而随意替换。
- validate 只看 possibleInputs[0]，不是支持所有候选工具的一般算法。
- 对异常 NBT 中重复工具项、超出四种工具、非法组合，读取和应用的校验边界不同。
- expected 等待余物无超时；机器吞掉余物时任务会停住，这是物料守恒保护但缺少诊断。
- 当前持久化是控制器账本，不会保存第三方机器的实际运输状态；重载正确性仍依赖机器自己的存档。
- 与概率补单联合时，ProbabilityTools 查询 held=reserved+borrowed，避免补单再索取已经借出的同一份工具。

测试映射见 08；不要把受控供应器测试等同于任意量子合成器/装配室总成验收。
