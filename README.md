# Nimble Pattern

[简体中文](README.zh.md)

Nimble Pattern is an addon for [Applied Energistics 2](https://github.com/AppliedEnergistics/Applied-Energistics-2),
adding some features for patterns to improve playing experiences.

## Requirement

- Minecraft `Version=1.20.1Forge`
- ae2 `Version>=15.4.10`

## Features

### Pattern Upgrade Terminal

This terminal can view all patterns in the network and support retrieving patterns from the terminal.
The patterns will go back to the original slot when it goes back to the terminal.
The terminal can set upgrade conditions for patterns, and it will send broadcasts to players when the upgrade condition
is achieved.

### Fuzzy Crafting

Add a upgrade slot of fuzzy card for pattern provider in AE2. The pattern provider with fuzzy card will execute fuzzy
matching for all inputs and outputs.

### Fake Crafting

The pattern is regarded as a fake pattern if a processing pattern only has a renamed book as output. When this pattern
executes, the crafting task will be regarded as successful as soon as all input materials are pushed by AE2 network,
which means it will not wait for the output.

## Compatible Mods

- jecharacters: Allow pinyin searches in pattern upgrade terminal.
- gtceu, gtlcore, gtladditions: Support the management of patterns in pattern buffer and molecular assembler matrix.

## Special Note

Mod `Inventory Tweaks Refoxed` will add a sorted button in the pattern upgrade terminal, which is useless. If you want
to remove this button,
add the following codes in the config file `invtweaks-client.toml`.

```toml
[[sorting.containerOverrides]]
containerClass = "com.ber.nimblePattern.client.gui.PatternUpgradeTermScreen"
sortRange = ""
[[sorting.containerOverrides]]
containerClass = "com.ber.nimblePattern.menu.PatternUpgradeTermMenu"
sortRange = ""
```

## License

Source code and assets are all follow the [GNU LGPL3.0](LICENSE) license.

This program uses the API of [Applied Energistics 2](https://github.com/AppliedEnergistics/Applied-Energistics-2).
Portions of the textures in this mod are derived and redrawn from
the [AE-Light-UI](https://github.com/LeeQianXi/AE-1.20-UI).