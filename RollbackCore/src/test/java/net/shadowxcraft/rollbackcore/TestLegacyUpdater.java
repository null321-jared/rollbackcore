package net.shadowxcraft.rollbackcore;
import org.junit.jupiter.api.Test;

public class TestLegacyUpdater {
    @Test
    public void TestModernMappings() {
        System.out.println(LegacyUpdater.getMapping("1-13-2", "1-20-2"));
    }
}
