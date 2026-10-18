package com.am.mcp.contract;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContractPackLoaderTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void loadsCoreGroupOnlyWhenPackPresent() throws Exception {
        Path root = fixtureRoot();
        ContractPackLoader loader = new ContractPackLoader(
                mapper,
                Set.of("am-news"),
                Set.of("core"),
                false);
        ContractPackLoader.LoadResult result = loader.load(root);
        assertEquals(1, result.tools().size());
        assertEquals("api_am_news_health", result.tools().get(0).name());
        assertEquals(1, result.toolsByService().get("am-news"));
        assertEquals("core", result.tools().get(0).group());
    }

    @Test
    void includeAllGroupsLoadsAdminToo() throws Exception {
        Path root = fixtureRoot();
        ContractPackLoader loader = new ContractPackLoader(
                mapper,
                Set.of("am-news"),
                Set.of("core"),
                true);
        ContractPackLoader.LoadResult result = loader.load(root);
        assertEquals(2, result.tools().size());
    }

    @Test
    void resolveBaseUrlPrefersEnv() throws Exception {
        Path root = fixtureRoot();
        ContractPackLoader loader = new ContractPackLoader(
                mapper, Set.of("am-news"), Set.of("core"), false);
        ContractToolSpec tool = loader.load(root).tools().get(0);
        assertTrue(tool.resolveBaseUrl("dev").contains("am-dev"));
        assertTrue(tool.resolveBaseUrl("prod").contains("am.asrax.in/news"));
    }

    private static Path fixtureRoot() throws Exception {
        URL url = ContractPackLoaderTest.class.getResource("/contract-packs/services/am-news/tools.json");
        assertTrue(url != null);
        return Path.of(url.toURI()).getParent().getParent().getParent();
    }
}
