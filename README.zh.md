# 灵巧样板

[English](README.md)

Nimble Pattern是[Applied Energistics 2](https://github.com/AppliedEnergistics/Applied-Energistics-2)
的附属模组，对AE2的样板增添了一些功能以提升游玩体验。

## 前置需求

- Minecraft `版本=1.20.1Forge`
- ae2 `版本>=15.4.10`

## 功能

### 样板更新终端

该终端可查看网络内所有样板，支持直接从终端取出样板，返回终端时样板会回到原始槽位。终端可以设置样板的更新条件，并在更新条件达成时发送消息提醒玩家。

### 模糊合成

为AE2的样板供应器添加了一个模糊卡的升级槽位，装配了模糊升级的样板供应器执行合成时会对输入和输出都执行模糊匹配。

### 假合成

若一个处理样板的产物只有一本被铁砧重命名过的书，则该样板被视为假样板。该样板执行时只要所有输入材料均已被AE2网络成功推送出去，则视为执行完成，不等待产物返回。

## 兼容模组

- jecharacters: 样板更新终端内可使用拼音进行搜索
- gtceu, gtlcore, gtladditions: 支持对样板总成系列和分子操纵者的样板管理

## 特别说明

模组`Inventory Tweaks Refoxed`会在样板更新终端添加一个无用的排序按钮。如果你想移除，将下面的代码添加到config的
`invtweaks-client.toml`文件即可。

```toml
[[sorting.containerOverrides]]
containerClass = "com.ber.nimblePattern.client.gui.PatternUpgradeTermScreen"
sortRange = ""
[[sorting.containerOverrides]]
containerClass = "com.ber.nimblePattern.menu.PatternUpgradeTermMenu"
sortRange = ""
```

## 许可证

源码和材质均遵循[GNU LGPL3.0](LICENSE)协议开源