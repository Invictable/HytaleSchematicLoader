# Hytale Schematic Loader

A basic plugin that loads and pastes worldedit schematics in Hytale

## Commands

- `/schem list` - Lists all schematics
- `/schem load <name>` - Loads a specific schematic
- '/schem paste' - paste recently loaded schematic relative to the player's location

## Info

Currently supports schematics created with WorldEdit on Minecraft 1.8
Will soon support Sponge 3 schematics (modern minecraft worldedit / sponge format)
(it might already, i wrote the code but havent tested it)

Current Issues:
- rotations are stored, but not applied to hytale blocks
- slab top / bottom information doesnt load
- Not all block conversions are supported

## Setup

Add schematics to mods -> cc.invic_schematic-loader -> schematics
Restart server after adding new schematics

## Material Conversions

Legacy Schematics map from id:data to namespace:itemname
from there modern minecraft maps from namespace:itemname to a Hytale block string
Modern schematics directly map from namespace:itemname to Hytale block strings

Legacy schematics use data for both rotations and color info.
ex. stained clay data is for color, but chest data is for rotation. when overriding legacy materials only enter the id. This makes the parser treat the data as a rotation

Under mods -> cc.invic_schematic-loader -> you can find hytale_overrides.txt and legacy_overrides.txt
You can specify override mappings for both legacy -> modern minecraft and modern minecraft -> hytale block string entries.  
This overrides the in code mappings, or lets you map modded minecraft items or unmapped items. If a mapping fails, stone will be placed.
Restart the server after editing.
