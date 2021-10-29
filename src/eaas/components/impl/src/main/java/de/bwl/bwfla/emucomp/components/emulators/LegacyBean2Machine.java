package de.bwl.bwfla.emucomp.components.emulators;

import java.util.HashMap;
import java.util.Map;

public class LegacyBean2Machine {
     public static final Map<String, String> emulatorContainerMap = new HashMap<String, String>() {{
        put("Qemu", "qemu-system");
        put("BasiliskII", "basiliskII");
        put("Beebem", "beebem");
        put("Hatari", "hatari");
        put("SheepShaver", "sheepshaver");
        put("ViceC64", "vice-sdl");
        put("Browser", "browser");
        put("VisualBoyAdvance", "visualboyadvance");
     }};

     public static final Map<String, String> emulatorMachineMap = new HashMap<String, String>() {{
        put("SheepShaver", "sheepshaver");
     }};
}
