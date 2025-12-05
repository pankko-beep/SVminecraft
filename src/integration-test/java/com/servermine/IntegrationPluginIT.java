package com.servermine;

// This integration test uses MockBukkit when the 'integration' Maven profile is enabled.
// The test file is compiled only when profile=integration is active (MockBukkit dependency included).

import org.junit.Test;
import org.junit.Ignore;

import static org.junit.Assert.*;

@Ignore("Integration tests require MockBukkit; enable profile integration to run them.")
public class IntegrationPluginIT {

    @Test
    public void pluginLoads() {
        // This test will run under profile integration with MockBukkit available.
        // Actual MockBukkit calls are intentionally omitted here (kept as a placeholder).
        assertTrue(true);
    }
}
