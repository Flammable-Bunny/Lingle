# Lingle

Lingle is an all purpose linux tool for mcsr, designed to setup and be used alongside **Tesselslate's [`Waywall`](https://github.com/tesselslate/waywall) / [`Resseti`](https://github.com/tesselslate/resetti)**


# auToMPFS

auToMPFS is a tool that automatically setus up TMPFS for you. 
- TMPFS is a temporary filesystem that stores files in your RAM instead of your SSD. This offers extremely fast file writing, but causes anything written with tmpfs to be deleted upon a system reboot or unmounting of the drive. This usually gives about 5-10 more rps on seedqueue depending on how fast your ram is.

The app works by creating and mounting `~/Lingle` with tmpfs, then telling your instances to write there instead of

`~/.local/share/PrismLauncher/instances/<instance>/saves`

### Easy TMPFS
- **ENABLE:**    Instantly mounts `~/Lingle` with tmpfs, and edits `/etc/fstab` to enable easy and instant mounting of tmpfs.
- **DISABLE:**   Instantly unmounts `~/Lingle`, and removes the automount from `/etc/fstab`. Allows for easy and instant disabling of tmpfs.


### Instance Linking
Detects instances in `~/.local/share/PrismLauncher/instances` (default prismlauncher path), and allows you to automatically symlink them to numbered files within `~/Lingle/`

### Linking Practice Map Progression Across Instaces (Does not Require TMPFS)
Detects world files in `~/.local/share/lingle/saves`, and creates folders that point to the worlds real files in your instances saves files. This enables 


# ADW (Auto Delete Worlds)
ADW is a tool that automatically deletes world folders from `~/Lingle`, preventing your ram from filling up and crashing your instance.
- **Customizable Interval:** Allows you to quickly and easially change how often the app deletes your worlds.
- **SUB 10 PACKAGE SAVER:** Ignores the 6 most recent worlds in your instances, so that you can verify any sub 10 Any% runs on speedrun.com
    - "[sub-10 runs must submit world files, evidence of past attempts, previous world files (5 preferable), logs, and gameplay audio (if using SeedQueue, you must submit **5 worlds before** and all worlds after the run)](https://www.speedrun.com/mc?h=Any_Glitchless-random-seed-1-16-1-19&rules=category&x=mkeyl926-r8rg67rn.21d4zvp1-wl33kewl.4qye4731)"


# SETUP

### auToMPFS: (PLEASE DO THE FOLLOWING ACTIONS IN THE ORDER THEY ARE LISTED!)
- Move all your practice map world files to `~/.local/share/lingle/saves/` (restart app if you need, as the maps list only updates when you start the app)
- Select every instance you want to enable tmpfs for, and press "Symlink Instances" Please note that **as of v0.5 there is no remove instance option**
- Select all practice maps you want to link across instances, and click "Link Practice Maps"
- Press "Auto Delete Worlds" to enable ADW, and instantly start it. 
- Now you must edit your init.lua so that it starts Lingle, and AWD in the background.

### Editing .lua for ADW
You need to update your init.lua so that Lingle ADW can run in the background.
for example if your add paths look something like: **(im assuming your using Gore's Generic config file or something similar)**
```lua
    local pacem_path = "/home/<user>/Documents/paceman-tracker-0.7.1.jar"
    local nb_path = "/home/<user>/Documents/Ninjabrain-Bot-1.5.1.jar"
    local overlay_path = "/home/<user>/.config/waywall/measuring_overlay.png"
```
you need to add the following line below them
```lua
    local lingle_path = "/home/<user>/Documents/Lingle-0.5.4.jar" #(make sure version is correct)
```
Then scroll down to where the NBB and Paceman stuff is, and add the following lines:
```lua
--*********************************************************************************************** LINGLE
local is_lingle_running = function()
	local handle = io.popen("pgrep -f 'lingle..*'")
	local result = handle:read("*l")
	handle:close()
	return result ~= nil
end

local exec_lingle = function()
	if not is_lingle_running() then
		waywall.exec("java -jar " .. lingle_path .. " --nogui")
	end
end
```
Then finnaly at the very bottom of your init.lua you should have this block:
```lua
	[open_ninbot_key] = function()
		exec_ninb()
		exec_pacem()
	end,
```
add the following line inside the block
```lua
exec_lingle()
```
**There are examples for what all of the blocks should look like after they are edited [here](https://github.com/Flammable-Bunny/Lingle/blob/master/exampleblocks.md)**

Please ask for help in the [Lingle discord server](https://discord.gg/9pQDfQbfXp) if you need any help

#
And finally, a big thanks to [Saanvi](https://github.com/its-saanvi) for creating the original guide for TMPFS, which inspired me to make auToMPFS.
- You can check out her original TMPFS guide [here](https://its-saanvi.github.io/linux-mcsr/perf/tmpfs.html)
