package com.example.gymmanagement.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Date;

public class DateKeysTest {
    @Test
    public void dateKeyUsesVietnamTime() {
        // 2026-03-01T17:30:00Z = 2026-03-02 00:30 giờ Việt Nam (UTC+7)
        Date d = new Date(java.time.Instant.parse("2026-03-01T17:30:00Z").toEpochMilli());
        assertEquals("20260302", DateKeys.keyOf(d));
    }

    @Test
    public void dateKeyBeforeMidnightVietnam() {
        Date d = new Date(java.time.Instant.parse("2026-03-01T16:59:59Z").toEpochMilli());
        assertEquals("20260301", DateKeys.keyOf(d));
    }

    @Test
    public void addDays() {
        Date d = new Date(0);
        assertEquals(7L * 86400_000L, DateKeys.addDays(d, 7).getTime() - d.getTime(), 3600_000L);
    }
}
