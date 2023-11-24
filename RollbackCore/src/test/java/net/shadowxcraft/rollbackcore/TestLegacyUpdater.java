package net.shadowxcraft.rollbackcore;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

public class TestLegacyUpdater {
    @Test
    public void TestModernMappings() throws IOException, InvalidConfigurationException {
        YamlConfiguration testSection = new YamlConfiguration();
        testSection.load(new File("src/main/resources/new_mappings.yml"));
        LegacyUpdater.loadModernMappingsConfig(Logger.getGlobal(), testSection);

        // Versions without any changes
        var unchangedMappings = LegacyUpdater.getMapping("1.17", "1.20.2");
        assertNotNull(unchangedMappings);
        assert unchangedMappings.isEmpty();
        // All changes up to 1.20.2
        var upTo1_20_2 = LegacyUpdater.getMapping("1.13", "1.20.2");
        assertEquals(2, Objects.requireNonNull(upTo1_20_2).size());

        // Validate null on backwards
        var backwards = LegacyUpdater.getMapping("1.20.2", "1.13");
        assertNull(backwards);

        // Validate null on too new of a version
        var tooNew = LegacyUpdater.getMapping("1.13", "1.999");
        assertNull(tooNew);

    }
}
