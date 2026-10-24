# Nimble Pattern

[简体中文](README.zh.md)

Nimble Pattern is an addon for [Applied Energistics 2](https://github.com/AppliedEnergistics/Applied-Energistics-2)
that adds new pattern features to improve the crafting experience.

## Requirements

- Minecraft Forge `1.20.1`
- AE2 `>=15.4.10`
- JEI `>=15.20.0.129`

## Features

### Pattern Tag Terminal

View all patterns in the network and retrieve them directly from the terminal.
Returning a pattern to the terminal places it back in its original slot.
The grid layout displays all network patterns together, while the grouped layout organizes them by their upgrade
conditions.

### Pattern Upgrades

Use the Pattern Tag Terminal to assign an upgrade condition to a pattern.
When the specified item enters the AE2 network, players carrying a wireless terminal receive a notification that
patterns are ready to upgrade.

In the grouped layout, an **Upgrade** button appears on the right side of a group's header when its upgrade condition is
met.
Click it to view suggested replacement recipes. Recipes for the same machine type take priority over those for other
machines.
Within each group of candidates, lower input cost (the simplified crafting-byte metric) takes priority over higher
output quantity.

**Recipe recommendations currently support GT recipes only.**

### Fuzzy Crafting

Adds a Fuzzy Card upgrade slot to AE2 Pattern Providers.
A Pattern Provider equipped with a Fuzzy Card uses fuzzy matching for both inputs and outputs.

### Fake Patterns

A processing pattern whose only output is a book renamed in an anvil is treated as a fake pattern.
Its crafting task completes as soon as the AE2 network has successfully dispatched all inputs, without waiting for the
output.

Encoding the same pattern again removes its fake-pattern tag.

### Loop Patterns

**Loop patterns support processing patterns only.**

For example, consider the following recipes in a Hypercube production loop:

```text
10 B Tachyon-Rich Temporal Fluid -> 10 B Spatial Expansion Fluid
1 B Spatial Expansion Fluid -> 64 Hypercubes
1 Hypercube -> 500 mB Tachyon-Rich Temporal Fluid
```

First, balance the recipes so their quantities match across the entire loop:

```text
10 B Tachyon-Rich Temporal Fluid -> 10 B Spatial Expansion Fluid -> 640 Hypercubes -> 320 B Tachyon-Rich Temporal Fluid
```

In this example, the loop's input is `Tachyon-Rich Temporal Fluid`, and its output is `Hypercubes`.
Select the input and output in the Pattern Tag Terminal, then insert a `Loop Storage Cell` to tag the patterns as loop
patterns.

#### Loop Storage Cells

Loop Storage Cells reserve the input material required by a loop.
In the example above, the cell is configured to store `10 B of Tachyon-Rich Temporal Fluid`.

When the network contains other storage cells, the contents of Loop Storage Cells are hidden from the terminal.
Their contents are visible only when the network contains exclusively Loop Storage Cells.

Materials can be loaded into or unloaded from a Loop Storage Cell only through an `ME IO Port`.
Players and ordinary AE2 crafting tasks cannot extract its contents.
AE2 accesses these reserved materials only when executing the corresponding loop craft.

### Tool Patterns

**Tool patterns support crafting patterns only.**

When encoding a pattern in the `Pattern Encoding Terminal`, an input item that is neither consumed nor damaged during
crafting
causes the pattern to be automatically tagged as a `Tool Pattern`.
Encoding the same pattern again removes the tag.

Players can also apply the tag manually in the `Pattern Tag Terminal`, provided the tool is neither consumed nor
damaged.
Besides conventional tools, certain recipes that duplicate an input item, such as `Netherite Upgrade Smithing Template`
duplication,
can also be tagged as tool patterns.

### Probability Patterns

In the `Pattern Tag Terminal`, set a maximum waiting time in seconds to tag a pattern as a `Probability Pattern`.
For example, with a waiting time of 5 seconds, ordering 10 probabilistic outputs starts a `5 × 10 = 50 second` timer
after AE2 dispatches the required materials.

If fewer than 10 outputs have arrived when the timer expires, AE2 automatically requests the missing quantity again.
This repeats until all 10 outputs have been obtained or insufficient materials prevent another request.

## Compatible Mods

- jecharacters: Enables Pinyin searches in the Pattern Tag Terminal.
- gtceu, gtlcore, gtladditions, extendedAE: Supports pattern management in the Pattern Buffer family,
  Molecular Assemblers, Extended Pattern Providers, and Assembler Matrices.

## Special Note

`Inventory Tweaks Refoxed` adds an unnecessary sorting button to the Pattern Tag Terminal.
To remove it, add the following entries to `config/invtweaks-client.toml`:

```toml
[[sorting.containerOverrides]]
containerClass = "com.ber.nimblePattern.client.gui.PatternTagTermScreen"
sortRange = ""

[[sorting.containerOverrides]]
containerClass = "com.ber.nimblePattern.menu.PatternTagTermMenu"
sortRange = ""
```

## License

The source code and assets are released under the [GNU LGPLv3](LICENSE).

This mod uses the APIs of [Applied Energistics 2](https://github.com/AppliedEnergistics/Applied-Energistics-2)
and [JEI](https://github.com/mezz/JustEnoughItems).
Some textures are taken from or adapted from [AE-Light-UI](https://github.com/LeeQianXi/AE-1.20-UI),
while others were designed based on its visual style.
