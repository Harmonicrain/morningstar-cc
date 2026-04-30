package com.eu.habbo.habbohotel.catalog;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CatalogPageModeTest {
    @Test
    void defaultsUnknownModesToNormal() {
        assertEquals(CatalogPageMode.NORMAL, CatalogPageMode.fromClientMode(null));
        assertEquals(CatalogPageMode.NORMAL, CatalogPageMode.fromClientMode("something_else"));
    }

    @Test
    void resolvesBuildersClubModeFromClientText() {
        assertEquals(CatalogPageMode.BUILDERS_CLUB, CatalogPageMode.fromClientMode("BUILDERS_CLUB"));
        assertEquals(CatalogPageMode.BUILDERS_CLUB, CatalogPageMode.fromClientMode("builders_club"));
    }
}
