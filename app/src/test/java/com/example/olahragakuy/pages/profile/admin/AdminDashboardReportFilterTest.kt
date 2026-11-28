package com.example.olahragakuy.pages.profile.admin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminDashboardReportFilterTest {

    private val reports = listOf(
        AdminReportItem(
            id = "1",
            reportedUsername = "BibiOlahraga",
            reporterUsername = "PecintaJogging",
            reportCategory = "Kekerasan, kebencian atau eksploitasi",
            reportDate = "30 Oktober 2026",
            type = AdminReportType.POST,
            withinLastHour = true,
            contentPreview = "Always on Proces",
        ),
        AdminReportItem(
            id = "2",
            reportedUsername = "PelariSantai",
            reporterUsername = "AdminHelper",
            reportCategory = "Penipuan, penggelapan atau spam",
            reportDate = "31 Oktober 2026",
            type = AdminReportType.COMMENT,
            withinLastHour = false,
            contentPreview = "Promo suplemen gratis",
        ),
        AdminReportItem(
            id = "3",
            reportedUsername = "GorMerdeka",
            reporterUsername = "RunnerBandung",
            reportCategory = "Hoax atau informasi palsu",
            reportDate = "31 Oktober 2026",
            type = AdminReportType.LOCATION,
            withinLastHour = true,
            contentPreview = "Alamat tidak valid",
        ),
        AdminReportItem(
            id = "4",
            reportedUsername = "AkunPalsu99",
            reporterUsername = "BibiOlahraga",
            reportCategory = "Pelanggaran hak cipta",
            reportDate = "31 Oktober 2026",
            type = AdminReportType.ACCOUNT,
            withinLastHour = false,
            contentPreview = "Duplikasi akun",
        ),
    )

    @Test
    fun defaultFiltersReturnAllReports() {
        val result = filterAdminReports(
            reports = reports,
            query = "",
            filter = AdminReportFilterState(),
        )
        assertEquals(4, result.size)
    }

    @Test
    fun selectingSpecificTypesFiltersCorrectly() {
        val onlyPost = filterAdminReports(
            reports = reports,
            query = "",
            filter = AdminReportFilterState(selectedTypes = setOf(AdminReportType.POST)),
        )
        assertEquals(listOf("1"), onlyPost.map { it.id })

        val postAndComment = filterAdminReports(
            reports = reports,
            query = "",
            filter = AdminReportFilterState(
                selectedTypes = setOf(AdminReportType.POST, AdminReportType.COMMENT),
            ),
        )
        assertEquals(listOf("1", "2"), postAndComment.map { it.id })

        val locationAndAccount = filterAdminReports(
            reports = reports,
            query = "",
            filter = AdminReportFilterState(
                selectedTypes = setOf(AdminReportType.LOCATION, AdminReportType.ACCOUNT),
            ),
        )
        assertEquals(listOf("3", "4"), locationAndAccount.map { it.id })

        val none = filterAdminReports(
            reports = reports,
            query = "",
            filter = AdminReportFilterState(selectedTypes = emptySet()),
        )
        assertTrue(none.isEmpty())
    }

    @Test
    fun categoryTimeAndQueryCombineCorrectly() {
        val byCategories = filterAdminReports(
            reports = reports,
            query = "",
            filter = AdminReportFilterState(
                selectedCategories = setOf("Penipuan, penggelapan atau spam", "Pelanggaran hak cipta"),
            ),
        )
        assertEquals(listOf("2", "4"), byCategories.map { it.id })

        val onlyLastHour = filterAdminReports(
            reports = reports,
            query = "",
            filter = AdminReportFilterState(onlyLastHour = true),
        )
        assertEquals(listOf("1", "3"), onlyLastHour.map { it.id })

        val combined = filterAdminReports(
            reports = reports,
            query = "  pecintajogging ",
            filter = AdminReportFilterState(onlyLastHour = true),
        )
        assertEquals(listOf("1"), combined.map { it.id })
    }
}
