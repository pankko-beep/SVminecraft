package com.servermine;

import com.servermine.storage.DatabaseManager;
import org.junit.Test;

import java.sql.SQLException;
import java.util.*;

import static org.junit.Assert.*;

public class DatabaseManagerTest {

    @Test
    public void testCreateTablesAndUpserts() throws Exception {
        DatabaseManager db = new DatabaseManager("jdbc:sqlite::memory:");

        UUID u = UUID.randomUUID();
        db.upsertObjetivoProgress(u, "raridade", 7);
        Map<UUID, Map<String, Integer>> all = db.loadAllObjetivos();
        assertTrue(all.containsKey(u));
        assertEquals(7, (int) all.get(u).get("raridade"));

        // ranks
        UUID r = UUID.randomUUID();
        db.upsertRank(r, 123, 2);
        Map<UUID, DatabaseManager.Rank> ranks = db.loadAllRanks();
        assertTrue(ranks.containsKey(r));
        assertEquals(123, ranks.get(r).xp);
        assertEquals(2, ranks.get(r).level);

        // lojas / items
        UUID owner = UUID.randomUUID();
        List<DatabaseManager.ItemRecord> items = new ArrayList<>();
        items.add(new DatabaseManager.ItemRecord(0, "DIAMOND", 2));
        items.add(new DatabaseManager.ItemRecord(1, "IRON_INGOT", 5));
        db.upsertLoja(owner, "PlayerX");
        db.saveLojaItems(owner, items);

        List<DatabaseManager.ItemRecord> loaded = db.loadLojaItems(owner);
        assertEquals(2, loaded.size());
        assertEquals("DIAMOND", loaded.get(0).material);
        assertEquals(2, loaded.get(0).amount);
        assertEquals(1, loaded.get(1).slot);

        Map<UUID, List<DatabaseManager.ItemRecord>> allLojas = db.loadAllLojasItems();
        assertTrue(allLojas.containsKey(owner));
        assertEquals(2, allLojas.get(owner).size());

        db.close();
    }

    @Test
    public void testSaveLojaItemsReplace() throws SQLException {
        DatabaseManager db = new DatabaseManager("jdbc:sqlite::memory:");
        UUID owner = UUID.randomUUID();
        List<DatabaseManager.ItemRecord> items = new ArrayList<>();
        items.add(new DatabaseManager.ItemRecord(0, "DIAMOND", 1));
        db.saveLojaItems(owner, items);

        // replace with an updated list
        List<DatabaseManager.ItemRecord> items2 = new ArrayList<>();
        items2.add(new DatabaseManager.ItemRecord(0, "GOLD_INGOT", 3));
        items2.add(new DatabaseManager.ItemRecord(1, "IRON_INGOT", 4));
        db.saveLojaItems(owner, items2);

        List<DatabaseManager.ItemRecord> loaded = db.loadLojaItems(owner);
        assertEquals(2, loaded.size());
        assertEquals("GOLD_INGOT", loaded.get(0).material);
        assertEquals(3, loaded.get(0).amount);
        assertEquals("IRON_INGOT", loaded.get(1).material);

        db.close();
    }
}
