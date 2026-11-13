package com.diamondrush.arena;

import java.util.Locale;

/**
 * A named arena with exactly two starting positions. Arenas are plain data
 * objects so they can be serialized with Gson and edited by admins in game.
 */
public final class Arena {

    public String name;
    public SpawnPoint spawn1;
    public SpawnPoint spawn2;

    public Arena() {
    }

    public Arena(String name) {
        this.name = name;
    }

    public boolean isComplete() {
        return spawn1 != null && spawn2 != null && spawn1.isFinite() && spawn2.isFinite();
    }

    public SpawnPoint spawn(int slot) {
        return slot == 1 ? spawn1 : spawn2;
    }

    public void setSpawn(int slot, SpawnPoint point) {
        if (slot == 1) {
            spawn1 = point;
        } else {
            spawn2 = point;
        }
    }

    public String status() {
        return String.format(Locale.ROOT, "P1:%s  P2:%s",
                spawn1 == null ? "unset" : "set",
                spawn2 == null ? "unset" : "set");
    }
}
