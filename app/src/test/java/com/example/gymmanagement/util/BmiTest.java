package com.example.gymmanagement.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BmiTest {
    @Test
    public void calculatesBmiRoundedToTwoDecimals() {
        assertEquals(22.86, Bmi.calc(70, 175), 0.001);
        assertEquals(20.0, Bmi.calc(64.8, 180), 0.001);
    }

    @Test
    public void bmiMatchesFirestoreRuleTolerance() {
        // Rules cho phép sai lệch < 0.06 so với w / h^2
        double w = 83.7, h = 171.3;
        double exact = w / ((h / 100.0) * (h / 100.0));
        assertTrue(Math.abs(Bmi.calc(w, h) - exact) < 0.06);
    }

    @Test
    public void labelsFollowThresholds() {
        assertEquals("Thấp hơn mức tham khảo", Bmi.label(17));
        assertEquals("Trong mức tham khảo", Bmi.label(22));
        assertEquals("Hơi cao so với mức tham khảo", Bmi.label(24));
        assertEquals("Cao hơn mức tham khảo", Bmi.label(30));
    }

    @Test
    public void validatesRanges() {
        assertTrue(Bmi.validWeight(20));
        assertTrue(Bmi.validWeight(350));
        assertFalse(Bmi.validWeight(19.9));
        assertFalse(Bmi.validWeight(351));
        assertTrue(Bmi.validHeight(80));
        assertFalse(Bmi.validHeight(250.1));
        assertTrue(Bmi.validBodyFat(2));
        assertFalse(Bmi.validBodyFat(71));
    }
}
