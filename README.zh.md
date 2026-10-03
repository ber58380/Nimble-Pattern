# 灵巧样板

[English](README.md)

Nimble Pattern是[Applied Energistics 2](https://github.com/AppliedEnergistics/Applied-Energistics-2)
的附属模组，对AE2的样板增添了一些功能以提升游玩体验。

## 前置需求

- Minecraft `版本=1.20.1Forge`
- ae2 `版本>=15.4.10`

## 功能

### 概率样板

从 JEI 导入支持的概率配方时，将输出折算为基础概率的期望数量；必要时统一放大输入和输出。编码后显示“概率样板”，再次编码只取消标记，保留期望数量。概率合成失败后在原 CPU 内补单，缺料时广播并等待补料。超时默认 120 秒，可在配置文件中修改。详见[使用说明与兼容范围](docs/probability-patterns.md)。

### 样板更新终端

该终端可查看网络内所有样板，支持直接从终端取出样板，返回终端时样板会回到原始槽位。终端可以设置样板的更新条件，并在更新条件达成时发送消息提醒玩家。

### 工具样板

在样板标记终端的“工具样板”页签中，将合成样板放入左侧，使用右侧四个虚拟槽标记工具，然后点击“应用”。目前只支持 AE2 合成样板，不支持处理样板。

工具必须是配方输入，并且满足以下一种条件：具有 `Unbreakable` 标签且配方原样返还工具；或者工具同时是产物且有净增产（例如锻造模板复制）。所选工具会对左侧所有样板统一校验，任一样板不符合条件则整次应用不修改样板。

每个任务只预留单次配方需要的工具数量，不随合成次数倍增。工具在 CPU 与装配室之间复用，不会每轮写回存储元件，任务结束后才归还网络；同一份工具仍需等待上次合成返还才能再次使用。锻造模板等配方只按净增产计算交付数量。开始任务前，网络需要已有足量工具。

### 模糊合成

为AE2的样板供应器添加了一个模糊卡的升级槽位，装配了模糊升级的样板供应器执行合成时会对输入和输出都执行模糊匹配。

### 假合成

若一个处理样板的产物只有一本被铁砧重命名过的书，则该样板被视为假样板。该样板执行时只要所有输入材料均已被AE2网络成功推送出去，则视为执行完成，不等待产物返回。

## 兼容模组

- jecharacters: 样板更新终端内可使用拼音进行搜索
- gtceu, gtlcore, gtladditions, extendedAE: 支持对样板总成系列、分子操纵者、扩展样板供应器和装配矩阵的样板管理

## 特别说明

模组`Inventory Tweaks Refoxed`会在样板更新终端添加一个无用的排序按钮。如果你想移除，将下面的代码添加到config的
`invtweaks-client.toml`文件即可。

```toml
[[sorting.containerOverrides]]
containerClass = "com.ber.nimblePattern.client.gui.PatternTagTermScreen"
sortRange = ""
[[sorting.containerOverrides]]
containerClass = "com.ber.nimblePattern.menu.PatternTagTermMenu"
sortRange = ""
```

## 许可证

源码和材质均遵循[GNU LGPL3.0](LICENSE)协议开源。

本模组代码使用了[Applied Energistics 2](https://github.com/AppliedEnergistics/Applied-Energistics-2)
的API，材质文件使用了[AE-Light-UI](https://github.com/LeeQianXi/AE-1.20-UI)的部分文件并基于其设计了部分材质。
