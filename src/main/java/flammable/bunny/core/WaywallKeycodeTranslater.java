package flammable.bunny.core;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class WaywallKeycodeTranslater {

    private static final Map<Integer, String> KEY_MAP = new HashMap<>();
    private static final Map<String, String> MODIFIER_MAP = new HashMap<>();
    private static final Map<Integer, String> MOUSE_MAP = new HashMap<>();

    static {
        KEY_MAP.put(KeyEvent.VK_ESCAPE, "ESC");
        KEY_MAP.put(KeyEvent.VK_1, "1");
        KEY_MAP.put(KeyEvent.VK_2, "2");
        KEY_MAP.put(KeyEvent.VK_3, "3");
        KEY_MAP.put(KeyEvent.VK_4, "4");
        KEY_MAP.put(KeyEvent.VK_5, "5");
        KEY_MAP.put(KeyEvent.VK_6, "6");
        KEY_MAP.put(KeyEvent.VK_7, "7");
        KEY_MAP.put(KeyEvent.VK_8, "8");
        KEY_MAP.put(KeyEvent.VK_9, "9");
        KEY_MAP.put(KeyEvent.VK_0, "0");
        KEY_MAP.put(KeyEvent.VK_MINUS, "MINUS");
        KEY_MAP.put(KeyEvent.VK_EQUALS, "EQUAL");
        KEY_MAP.put(KeyEvent.VK_BACK_SPACE, "BACKSPACE");
        KEY_MAP.put(KeyEvent.VK_TAB, "TAB");
        KEY_MAP.put(KeyEvent.VK_Q, "Q");
        KEY_MAP.put(KeyEvent.VK_W, "W");
        KEY_MAP.put(KeyEvent.VK_E, "E");
        KEY_MAP.put(KeyEvent.VK_R, "R");
        KEY_MAP.put(KeyEvent.VK_T, "T");
        KEY_MAP.put(KeyEvent.VK_Y, "Y");
        KEY_MAP.put(KeyEvent.VK_U, "U");
        KEY_MAP.put(KeyEvent.VK_I, "I");
        KEY_MAP.put(KeyEvent.VK_O, "O");
        KEY_MAP.put(KeyEvent.VK_P, "P");
        KEY_MAP.put(KeyEvent.VK_OPEN_BRACKET, "LEFTBRACE");
        KEY_MAP.put(KeyEvent.VK_CLOSE_BRACKET, "RIGHTBRACE");
        KEY_MAP.put(KeyEvent.VK_BACK_SLASH, "BACKSLASH");
        KEY_MAP.put(KeyEvent.VK_CONTROL, "LEFTCTRL");
        KEY_MAP.put(KeyEvent.VK_A, "A");
        KEY_MAP.put(KeyEvent.VK_S, "S");
        KEY_MAP.put(KeyEvent.VK_D, "D");
        KEY_MAP.put(KeyEvent.VK_F, "F");
        KEY_MAP.put(KeyEvent.VK_G, "G");
        KEY_MAP.put(KeyEvent.VK_H, "H");
        KEY_MAP.put(KeyEvent.VK_J, "J");
        KEY_MAP.put(KeyEvent.VK_K, "K");
        KEY_MAP.put(KeyEvent.VK_L, "L");
        KEY_MAP.put(KeyEvent.VK_SEMICOLON, "SEMICOLON");
        KEY_MAP.put(KeyEvent.VK_QUOTE, "APOSTROPHE");
        KEY_MAP.put(KeyEvent.VK_ENTER, "ENTER");
        KEY_MAP.put(KeyEvent.VK_BACK_QUOTE, "GRAVE");
        KEY_MAP.put(KeyEvent.VK_SHIFT, "LEFTSHIFT");
        KEY_MAP.put(KeyEvent.VK_Z, "Z");
        KEY_MAP.put(KeyEvent.VK_X, "X");
        KEY_MAP.put(KeyEvent.VK_C, "C");
        KEY_MAP.put(KeyEvent.VK_V, "V");
        KEY_MAP.put(KeyEvent.VK_B, "B");
        KEY_MAP.put(KeyEvent.VK_N, "N");
        KEY_MAP.put(KeyEvent.VK_M, "M");
        KEY_MAP.put(KeyEvent.VK_COMMA, "COMMA");
        KEY_MAP.put(KeyEvent.VK_PERIOD, "DOT");
        KEY_MAP.put(KeyEvent.VK_SLASH, "SLASH");
        KEY_MAP.put(KeyEvent.VK_MULTIPLY, "KPASTERISK");
        KEY_MAP.put(KeyEvent.VK_ALT, "LEFTALT");
        KEY_MAP.put(KeyEvent.VK_SPACE, "SPACE");
        KEY_MAP.put(KeyEvent.VK_CAPS_LOCK, "CAPSLOCK");
        KEY_MAP.put(KeyEvent.VK_F1, "F1");
        KEY_MAP.put(KeyEvent.VK_F2, "F2");
        KEY_MAP.put(KeyEvent.VK_F3, "F3");
        KEY_MAP.put(KeyEvent.VK_F4, "F4");
        KEY_MAP.put(KeyEvent.VK_F5, "F5");
        KEY_MAP.put(KeyEvent.VK_F6, "F6");
        KEY_MAP.put(KeyEvent.VK_F7, "F7");
        KEY_MAP.put(KeyEvent.VK_F8, "F8");
        KEY_MAP.put(KeyEvent.VK_F9, "F9");
        KEY_MAP.put(KeyEvent.VK_F10, "F10");
        KEY_MAP.put(KeyEvent.VK_F11, "F11");
        KEY_MAP.put(KeyEvent.VK_F12, "F12");
        KEY_MAP.put(KeyEvent.VK_F13, "F13");
        KEY_MAP.put(KeyEvent.VK_F14, "F14");
        KEY_MAP.put(KeyEvent.VK_F15, "F15");
        KEY_MAP.put(KeyEvent.VK_F16, "F16");
        KEY_MAP.put(KeyEvent.VK_F17, "F17");
        KEY_MAP.put(KeyEvent.VK_F18, "F18");
        KEY_MAP.put(KeyEvent.VK_F19, "F19");
        KEY_MAP.put(KeyEvent.VK_F20, "F20");
        KEY_MAP.put(KeyEvent.VK_F21, "F21");
        KEY_MAP.put(KeyEvent.VK_F22, "F22");
        KEY_MAP.put(KeyEvent.VK_F23, "F23");
        KEY_MAP.put(KeyEvent.VK_F24, "F24");
        KEY_MAP.put(KeyEvent.VK_NUM_LOCK, "NUMLOCK");
        KEY_MAP.put(KeyEvent.VK_SCROLL_LOCK, "SCROLLLOCK");
        KEY_MAP.put(KeyEvent.VK_NUMPAD7, "KP7");
        KEY_MAP.put(KeyEvent.VK_NUMPAD8, "KP8");
        KEY_MAP.put(KeyEvent.VK_NUMPAD9, "KP9");
        KEY_MAP.put(KeyEvent.VK_SUBTRACT, "KPMINUS");
        KEY_MAP.put(KeyEvent.VK_NUMPAD4, "KP4");
        KEY_MAP.put(KeyEvent.VK_NUMPAD5, "KP5");
        KEY_MAP.put(KeyEvent.VK_NUMPAD6, "KP6");
        KEY_MAP.put(KeyEvent.VK_ADD, "KPPLUS");
        KEY_MAP.put(KeyEvent.VK_NUMPAD1, "KP1");
        KEY_MAP.put(KeyEvent.VK_NUMPAD2, "KP2");
        KEY_MAP.put(KeyEvent.VK_NUMPAD3, "KP3");
        KEY_MAP.put(KeyEvent.VK_NUMPAD0, "KP0");
        KEY_MAP.put(KeyEvent.VK_DECIMAL, "KPDOT");
        KEY_MAP.put(KeyEvent.VK_DIVIDE, "KPSLASH");
        KEY_MAP.put(KeyEvent.VK_PRINTSCREEN, "SYSRQ");
        KEY_MAP.put(KeyEvent.VK_HOME, "HOME");
        KEY_MAP.put(KeyEvent.VK_UP, "UP");
        KEY_MAP.put(KeyEvent.VK_PAGE_UP, "PAGEUP");
        KEY_MAP.put(KeyEvent.VK_LEFT, "LEFT");
        KEY_MAP.put(KeyEvent.VK_RIGHT, "RIGHT");
        KEY_MAP.put(KeyEvent.VK_END, "END");
        KEY_MAP.put(KeyEvent.VK_DOWN, "DOWN");
        KEY_MAP.put(KeyEvent.VK_PAGE_DOWN, "PAGEDOWN");
        KEY_MAP.put(KeyEvent.VK_INSERT, "INSERT");
        KEY_MAP.put(KeyEvent.VK_DELETE, "DELETE");
        KEY_MAP.put(KeyEvent.VK_PAUSE, "PAUSE");
        KEY_MAP.put(KeyEvent.VK_META, "LEFTMETA");
        KEY_MAP.put(KeyEvent.VK_WINDOWS, "LEFTMETA");
        KEY_MAP.put(KeyEvent.VK_CONTEXT_MENU, "COMPOSE");
        KEY_MAP.put(KeyEvent.VK_STOP, "STOP");
        KEY_MAP.put(KeyEvent.VK_AGAIN, "AGAIN");
        KEY_MAP.put(KeyEvent.VK_PROPS, "PROPS");
        KEY_MAP.put(KeyEvent.VK_UNDO, "UNDO");
        KEY_MAP.put(KeyEvent.VK_COPY, "COPY");
        KEY_MAP.put(KeyEvent.VK_PASTE, "PASTE");
        KEY_MAP.put(KeyEvent.VK_FIND, "FIND");
        KEY_MAP.put(KeyEvent.VK_CUT, "CUT");
        KEY_MAP.put(KeyEvent.VK_HELP, "HELP");


        MOUSE_MAP.put(1, "lmb");
        MOUSE_MAP.put(2, "mmb");
        MOUSE_MAP.put(3, "rmb");
        MOUSE_MAP.put(6, "mb4");
        MOUSE_MAP.put(7, "mb5");
        for (int i = 8; i <= 20; i++)
            MOUSE_MAP.put(i, "mb" + (i - 2));


        MODIFIER_MAP.put("shift", "Shift");
        MODIFIER_MAP.put("ctrl", "Ctrl");
        MODIFIER_MAP.put("control", "Ctrl");
        MODIFIER_MAP.put("alt", "Alt");
        MODIFIER_MAP.put("mod1", "Alt");
        MODIFIER_MAP.put("super", "Super");
        MODIFIER_MAP.put("mod4", "Super");
        MODIFIER_MAP.put("win", "Super");
        MODIFIER_MAP.put("meta", "Super");
    }

    public static String formatKeyEvent(KeyEvent e) {
        StringBuilder sb = new StringBuilder();
        int mods = e.getModifiersEx();

        if ((mods & InputEvent.CTRL_DOWN_MASK) != 0) sb.append("Ctrl-");
        if ((mods & InputEvent.ALT_DOWN_MASK) != 0) sb.append("Alt-");
        if ((mods & InputEvent.SHIFT_DOWN_MASK) != 0) sb.append("Shift-");
        if ((mods & InputEvent.META_DOWN_MASK) != 0) sb.append("Super-");

        String keyName = KEY_MAP.get(e.getKeyCode());
        if (keyName != null) {
            sb.append(keyName);
        } else {
            String keyText = KeyEvent.getKeyText(e.getKeyCode());
            keyText = keyText.replace(' ', '-').toUpperCase();
            sb.append(keyText);
        }

        return sb.toString();
    }


    public static String formatMouseEvent(MouseEvent e) {
        StringBuilder sb = new StringBuilder();
        int mods = e.getModifiersEx();

        if ((mods & InputEvent.CTRL_DOWN_MASK) != 0) sb.append("Ctrl-");
        if ((mods & InputEvent.ALT_DOWN_MASK) != 0) sb.append("Alt-");
        if ((mods & InputEvent.SHIFT_DOWN_MASK) != 0) sb.append("Shift-");
        if ((mods & InputEvent.META_DOWN_MASK) != 0) sb.append("Super-");

        String buttonName = MOUSE_MAP.get(e.getButton());
        if (buttonName != null) {
            sb.append(buttonName);
        } else {
            sb.append("mb").append(e.getButton());
        }

        return sb.toString();
    }

    public static boolean isModifierKey(int keyCode) {
        return keyCode == KeyEvent.VK_SHIFT ||
               keyCode == KeyEvent.VK_CONTROL ||
               keyCode == KeyEvent.VK_ALT ||
               keyCode == KeyEvent.VK_META ||
               keyCode == KeyEvent.VK_WINDOWS ||
               keyCode == KeyEvent.VK_CAPS_LOCK ||
               keyCode == KeyEvent.VK_NUM_LOCK;
    }


    public static boolean isValidKeybind(String keybind) {
        if (keybind == null || keybind.isBlank()) return false;

        String[] parts = keybind.split("-");
        if (parts.length == 0) return false;

        String lastPart = parts[parts.length - 1].toLowerCase();
        return !MODIFIER_MAP.containsKey(lastPart);
    }


    public static Set<String> getValidKeyNames() {
        return Set.copyOf(KEY_MAP.values());
    }


    public static Set<String> getValidMouseButtons() {
        return Set.copyOf(MOUSE_MAP.values());
    }
}
