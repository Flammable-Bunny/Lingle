#FIRST BLOCK EXAPLE (no context you got this)
local pacem_path = "/home/<user>/Documents/paceman-tracker-0.7.1.jar"
local nb_path = "/home/<user>/Documents/Ninjabrain-Bot-1.5.1.jar"
local lingle_path = "/home/<user>/Documents/Lingle-0.5.4.jar"
local overlay_path = "/home/<user>/.config/waywall/measuring_overlay.png"

#SECOND BLOCK (with context) ((your addding the middle one))

--*********************************************************************************************** PACEMAN
local is_pacem_running = function()
	local handle = io.popen("pgrep -f 'paceman..*'")
	local result = handle:read("*l")
	handle:close()
	return result ~= nil
end

local exec_pacem = function()
	if not is_pacem_running() then
		waywall.exec("java -jar " .. pacem_path .. " --nogui")
	end
end

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

--*********************************************************************************************** NINJABRAIN
local is_ninb_running = function()
	local handle = io.popen("pgrep -f 'Ninjabrain.*jar'")
	local result = handle:read("*l")
	handle:close()
	return result ~= nil
end

local exec_ninb = function()
	if not is_ninb_running() then
		waywall.exec("java -Dawt.useSystemAAFontSettings=on -jar " .. nb_path)
	end
end

#THIRD BLOCK EXAMPLE

	[open_ninbot_key] = function()
		exec_ninb()
		exec_pacem()
		exec_lingle()
	end,
