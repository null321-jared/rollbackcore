package net.shadowxcraft.rollbackcore;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TestRollbackOperation {
    @Test
    public void TestSignIdentification() {
        System.out.println(RollbackOperation.specialBlockStates);
        assert RollbackOperation.specialBlockStates.contains(Material.OAK_SIGN);
        assert RollbackOperation.specialBlockStates.contains(Material.OAK_WALL_SIGN);
        assert RollbackOperation.specialBlockStates.contains(Material.OAK_HANGING_SIGN);
        assert RollbackOperation.specialBlockStates.contains(Material.OAK_WALL_HANGING_SIGN);
        assert RollbackOperation.specialBlockStates.contains(Material.MANGROVE_SIGN);
        assert RollbackOperation.specialBlockStates.contains(Material.MANGROVE_WALL_SIGN);
        assert RollbackOperation.specialBlockStates.contains(Material.MANGROVE_HANGING_SIGN);
        assert RollbackOperation.specialBlockStates.contains(Material.MANGROVE_WALL_HANGING_SIGN);
        assert RollbackOperation.specialBlockStates.contains(Material.PLAYER_HEAD);
        assert RollbackOperation.specialBlockStates.contains(Material.PLAYER_WALL_HEAD);
        assert RollbackOperation.specialBlockStates.contains(Material.COMMAND_BLOCK);
    }
}
