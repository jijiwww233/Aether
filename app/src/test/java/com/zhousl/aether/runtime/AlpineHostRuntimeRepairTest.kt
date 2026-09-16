package com.zhousl.aether.runtime

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlpineHostRuntimeRepairTest {
    @Test
    fun detectsMissingHostExecutableWhenRootfsWasInstalled() {
        assertTrue(
            isAlpineHostRuntimeIncomplete(
                rootfsInstalled = true,
                prootInstalled = true,
                loaderInstalled = false,
                libTallocInstalled = true,
            )
        )
    }

    @Test
    fun doesNotTreatAnUninstalledRootfsAsAHostRuntimeRepair() {
        assertFalse(
            isAlpineHostRuntimeIncomplete(
                rootfsInstalled = false,
                prootInstalled = false,
                loaderInstalled = false,
                libTallocInstalled = false,
            )
        )
    }

    @Test
    fun doesNotRepairACompleteHostRuntime() {
        assertFalse(
            isAlpineHostRuntimeIncomplete(
                rootfsInstalled = true,
                prootInstalled = true,
                loaderInstalled = true,
                libTallocInstalled = true,
            )
        )
    }
}
