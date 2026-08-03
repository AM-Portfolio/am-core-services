package com.am.mcp.tools;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PortfolioToolsBlankToNullTest {

    @Test
    void blankToNull_mapsBlankAndNull() {
        assertNull(PortfolioTools.blankToNull(null));
        assertNull(PortfolioTools.blankToNull(""));
        assertNull(PortfolioTools.blankToNull("  "));
        assertEquals("abc", PortfolioTools.blankToNull("abc"));
    }
}
