package com.eu.habbo.habbohotel.catalog;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildersClubCatalogRegistryTest {
    @Test
    void parsesBuildersClubCompatibleFurnitureFromFurnidata() throws Exception {
        String xml = """
                <furnidata>
                  <roomitemtypes>
                    <furnitype id="5574" classname="bc_lavarock*7">
                      <offerid>5574</offerid>
                      <bc>1</bc>
                    </furnitype>
                    <furnitype id="6000" classname="normal_item">
                      <offerid>6000</offerid>
                      <bc>0</bc>
                    </furnitype>
                  </roomitemtypes>
                  <wallitemtypes>
                    <wallitemtype id="7000" classname="poster_test">
                      <offerid>7100</offerid>
                      <bc>1</bc>
                    </wallitemtype>
                  </wallitemtypes>
                </furnidata>
                """;

        BuildersClubCatalogRegistry registry = BuildersClubCatalogRegistry.parse(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))
        );

        assertTrue(registry.isCompatible(5574, 5574));
        assertTrue(registry.isCompatible(7000, 7100));
        assertFalse(registry.isCompatible(6000, 6000));
        assertFalse(registry.isCompatible(5574, 9999));
    }
}
