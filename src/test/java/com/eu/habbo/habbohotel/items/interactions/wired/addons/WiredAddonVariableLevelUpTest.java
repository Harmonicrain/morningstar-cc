package com.eu.habbo.habbohotel.items.interactions.wired.addons;

import com.eu.habbo.habbohotel.items.interactions.wired.WiredSettingsV2;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WiredAddonVariableLevelUpTest {
    @Test
    void savesTheThreeExactJulyPayloadShapes() {
        WiredAddonVariableLevelUp addon = addon();

        assertTrue(addon.saveData(settings(new int[] {0xA5, 0}, "2=100\n4=400")));
        assertArrayEquals(new int[] {0xA5, 0}, addon.getWiredIntParams());
        assertEquals("2=100\n4=400", addon.getWiredStringParam());

        assertTrue(addon.saveData(settings(new int[] {7, 1, 250, 10}, "")));
        assertArrayEquals(new int[] {7, 1, 250, 10}, addon.getWiredIntParams());
        assertEquals("", addon.getWiredStringParam());

        assertTrue(addon.saveData(settings(new int[] {3, 2, 100, 20, 10}, "")));
        assertArrayEquals(new int[] {3, 2, 100, 20, 10}, addon.getWiredIntParams());
        assertEquals("", addon.getWiredStringParam());
    }

    @Test
    void rejectsMalformedOrOutOfScopePayloadsWithoutMutatingTheConfiguration() {
        WiredAddonVariableLevelUp addon = addon();
        assertTrue(addon.saveData(settings(new int[] {3, 1, 100, 10}, "")));

        assertFalse(addon.saveData(settings(new int[] {3, 0, 100}, "2=100")));
        assertFalse(addon.saveData(settings(new int[] {3, 1, 100, 10}, "unexpected")));
        assertFalse(addon.saveData(settings(new int[] {256, 1, 100, 10}, "")));
        assertFalse(addon.saveData(new WiredSettingsV2(new int[] {3, 1, 100, 10}, "", new int[] {9},
                new int[0], new String[0], new int[0], new int[0], 0, 0, false, false)));
        assertFalse(addon.saveData(settings(new int[] {3, 0}, "2=100\nnot-a-line")));

        assertArrayEquals(new int[] {3, 1, 100, 10}, addon.getWiredIntParams());
    }

    @Test
    void linearAndExponentialCalculationsMatchTheLevelUpDonorSemantics() {
        WiredAddonVariableLevelUp.LevelConfiguration linear =
                WiredAddonVariableLevelUp.LevelConfiguration.linear(0, 100, 3);
        WiredAddonVariableLevelUp.LevelState linearAtSecondLevel = WiredAddonVariableLevelUp.calculate(linear, 150);
        assertEquals(2, linearAtSecondLevel.currentLevel());
        assertEquals(50, linearAtSecondLevel.progress());
        assertEquals(50, linearAtSecondLevel.progressPercentage());
        assertEquals(100, linearAtSecondLevel.xpRequired());
        assertEquals(50, linearAtSecondLevel.xpRemaining());
        assertFalse(linearAtSecondLevel.isMaxed());

        WiredAddonVariableLevelUp.LevelState linearAtCap = WiredAddonVariableLevelUp.calculate(linear, 999);
        assertEquals(3, linearAtCap.currentLevel());
        assertEquals(300, linearAtCap.currentXp());
        assertTrue(linearAtCap.isMaxed());

        WiredAddonVariableLevelUp.LevelConfiguration exponential =
                WiredAddonVariableLevelUp.LevelConfiguration.exponential(0, 100, 20, 3);
        WiredAddonVariableLevelUp.LevelState exponentialAtLevelThree =
                WiredAddonVariableLevelUp.calculate(exponential, 220);
        assertEquals(3, exponentialAtLevelThree.currentLevel());
        assertEquals(144, exponentialAtLevelThree.xpRemaining());
        assertEquals(144, exponentialAtLevelThree.xpRequired());
    }

    @Test
    void manualConfigurationUsesTheDonorThresholdAndCapRules() {
        WiredAddonVariableLevelUp.LevelConfiguration manual =
                WiredAddonVariableLevelUp.LevelConfiguration.manual(0, "2=100\n4=400");
        WiredAddonVariableLevelUp.LevelState middle = WiredAddonVariableLevelUp.calculate(manual, 200);
        assertEquals(3, middle.currentLevel());
        assertEquals(300, middle.xpRequired());
        assertEquals(200, middle.xpRemaining());
        assertFalse(middle.isMaxed());

        WiredAddonVariableLevelUp.LevelState cap = WiredAddonVariableLevelUp.calculate(manual, 401);
        assertEquals(4, cap.currentLevel());
        assertEquals(400, cap.currentXp());
        assertTrue(cap.isMaxed());
    }

    @Test
    void versionedPersistenceRoundTripsAndRejectsInvalidVersions() {
        WiredAddonVariableLevelUp source = addon();
        assertTrue(source.saveData(settings(new int[] {12, 0}, "2=100\r\n5=900")));
        String persisted = source.getWiredData();
        assertTrue(persisted.contains("\"v\":1"));

        WiredAddonVariableLevelUp restored = addon();
        assertTrue(restored.loadPersistedData(persisted));
        assertArrayEquals(new int[] {12, 0}, restored.getWiredIntParams());
        assertEquals("2=100\n5=900", restored.getWiredStringParam());
        assertEquals(5, restored.calculate(900).currentLevel());

        assertFalse(restored.loadPersistedData("{\"v\":2,\"mode\":1,\"subvariableMask\":0,\"stepSize\":100,\"maxLevel\":2}"));
        assertFalse(restored.loadPersistedData("{\"v\":1,\"mode\":0,\"subvariableMask\":0,\"stepSize\":100,\"baseXp\":100,\"increaseFactor\":20,\"maxLevel\":50}"));
        assertArrayEquals(new int[] {12, 0}, restored.getWiredIntParams());
    }

    private static WiredAddonVariableLevelUp addon() {
        return new WiredAddonVariableLevelUp(1, 1, null, "0", 0, 0);
    }

    private static WiredSettingsV2 settings(int[] params, String text) {
        return new WiredSettingsV2(params, text, new int[0], new int[0], new String[0],
                new int[0], new int[0], 0, 0, false, false);
    }
}
