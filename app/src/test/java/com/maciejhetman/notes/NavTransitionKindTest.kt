package com.maciejhetman.notes

import androidx.compose.ui.unit.LayoutDirection
import com.maciejhetman.notes.navigation.NavDestinationKind
import com.maciejhetman.notes.navigation.NavTransitionKind
import com.maciejhetman.notes.navigation.navDestinationKind
import com.maciejhetman.notes.navigation.navTransitionKind
import com.maciejhetman.notes.ui.animation.Motion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NavTransitionKindTest {

    @Test
    fun `type-safe route strings map to destination kinds`() {
        assertEquals(
            NavDestinationKind.NoteList,
            navDestinationKind("com.maciejhetman.notes.navigation.Routes.NoteList/{folderId}")
        )
        assertEquals(
            NavDestinationKind.NoteDetail,
            navDestinationKind("com.maciejhetman.notes.navigation.Routes.NoteDetail/{noteId}/{folderId}")
        )
        assertEquals(
            NavDestinationKind.Settings,
            navDestinationKind("com.maciejhetman.notes.navigation.Routes.Settings")
        )
        assertEquals(NavDestinationKind.Unknown, navDestinationKind(null))
        assertEquals(NavDestinationKind.Unknown, navDestinationKind("SomewhereElse"))
    }

    @Test
    fun `folder and settings use hierarchical transitions`() {
        assertEquals(
            NavTransitionKind.Hierarchical,
            navTransitionKind(NavDestinationKind.NoteList, NavDestinationKind.NoteList)
        )
        assertEquals(
            NavTransitionKind.Hierarchical,
            navTransitionKind(NavDestinationKind.NoteList, NavDestinationKind.Settings)
        )
        assertEquals(
            NavTransitionKind.Hierarchical,
            navTransitionKind(NavDestinationKind.Settings, NavDestinationKind.NoteList)
        )
        // Different folderId args still classify as NoteList → NoteList hierarchical.
        assertEquals(
            NavTransitionKind.Hierarchical,
            navTransitionKind(
                "com.maciejhetman.notes.navigation.Routes.NoteList/?folderId=null",
                "com.maciejhetman.notes.navigation.Routes.NoteList/?folderId=42"
            )
        )
    }

    @Test
    fun `note detail uses container transform with list`() {
        assertEquals(
            NavTransitionKind.ContainerTransform,
            navTransitionKind(NavDestinationKind.NoteList, NavDestinationKind.NoteDetail)
        )
        assertEquals(
            NavTransitionKind.ContainerTransform,
            navTransitionKind(NavDestinationKind.NoteDetail, NavDestinationKind.NoteList)
        )
    }

    @Test
    fun `rtl layout sign mirrors hierarchical edge offsets`() {
        assertEquals(1, Motion.Hierarchical.layoutSign(LayoutDirection.Ltr))
        assertEquals(-1, Motion.Hierarchical.layoutSign(LayoutDirection.Rtl))

        val width = 400
        val ltrEnter = Motion.Hierarchical.layoutSign(LayoutDirection.Ltr) * width
        val rtlEnter = Motion.Hierarchical.layoutSign(LayoutDirection.Rtl) * width
        assertEquals(width, ltrEnter)
        assertEquals(-width, rtlEnter)

        val ltrParent = -Motion.Hierarchical.layoutSign(LayoutDirection.Ltr) * width /
            Motion.Hierarchical.PARENT_PARALLAX
        val rtlParent = -Motion.Hierarchical.layoutSign(LayoutDirection.Rtl) * width /
            Motion.Hierarchical.PARENT_PARALLAX
        assertEquals(-width / 4, ltrParent)
        assertEquals(width / 4, rtlParent)
        assertTrue(ltrParent != rtlParent)
    }

    @Test
    fun `hierarchical pop exit scale matches stock preview`() {
        assertEquals(0.9f, Motion.Hierarchical.POP_EXIT_SCALE, 0f)
        assertEquals(4, Motion.Hierarchical.PARENT_PARALLAX)
    }
}
