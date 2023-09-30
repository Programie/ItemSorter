# Changelog

## [1.4] - 2023-08-20

* Prevent editing sign with right click implemented with Minecraft 1.20
* Now requires at least Minecraft 1.20.1

## [1.3.1] - 2022-03-29

* Automatically start item transfers once a chunk gets loaded and on plugin load for loaded chunks
* Fixed transferring multiple items at once in some use cases

## [1.3] - 2022-03-28

* Only move one item at once (like hoppers)

## [1.2] - 2022-03-20

* New feature: Connect multiple chests using a single sign
* Fixed: Ensure default value is used if property is not defined in config.yml
* Fixed: Placing a sign far away from another sign with the same name tries to load the chunk if it is not loaded resulting in a lag spike
* Optimized performance while transferring items to target inventories

## [1.1] - 2022-03-14

* Fixed not closing database on plugin unload
* Allow to limit number of different names per player and signs per name

## [1.0] - 2022-03-13

Initial release