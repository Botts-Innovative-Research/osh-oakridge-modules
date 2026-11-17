/*******************************************************************************
 * Copyright (c) 2026 Botts Innovative Research, Inc.
 * All rights reserved.
 *******************************************************************************/
package com.botts.impl.process.ocr.domain;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class Iso6346Test {

    @Test
    void computesAndValidatesStandardCheckDigit() {
        assertEquals(3, Iso6346.computeCheckDigit("CSQU305438"));
        assertTrue(Iso6346.isValid("CSQU3054383"));
        assertFalse(Iso6346.isValid("CSQU3054384"));
    }

    @Test
    void coercesAmbiguousLettersAndDigitsByPosition() {
        Optional<Iso6346.Match> match = Iso6346.extractBest("CS0U 3O5438 3");
        assertTrue(match.isPresent());
        assertEquals("CSQU3054383", match.get().compact());
        assertEquals("CSQU 305438 3", match.get().formatted());
        assertTrue(match.get().checksumValid());
        assertFalse(match.get().checkDigitRepaired());
    }

    @Test
    void stripsBurnedInTimestampsBeforeContainerExtraction() {
        Optional<Iso6346.Match> match = Iso6346.extractBest("2026-11-14 00:25:12 CSQU 305438 3 CAM01");
        assertTrue(match.isPresent());
        assertEquals("CSQU3054383", match.get().compact());
        assertTrue(match.get().checksumValid());
    }

    @Test
    void repairsMissingBoxedCheckDigitWhenTenCharPrefixReadCleanly() {
        Optional<Iso6346.Match> match = Iso6346.extractBest("CSQU 305438");
        assertTrue(match.isPresent());
        assertEquals("CSQU3054383", match.get().compact());
        assertTrue(match.get().checksumValid());
        assertTrue(match.get().checkDigitRepaired());
    }

    @Test
    void rejectsDigitCoercedCategoryWhenCheckDigitDoesNotValidate() {
        // '0' at position 3 would coerce to 'U', and check digit '9' is wrong (expected '4'):
        // must be rejected so random alphanumeric strings do not masquerade as containers.
        Optional<Iso6346.Match> match = Iso6346.extractBest("ABC01234569");
        assertTrue(match.isEmpty());
    }

    @Test
    void repairsSingleCharBicOwnerConfusionAgainstPrintedCheckDigitWithoutOverwritingSerialErrors() {
        // Printed check digit validates unique BIC owner neighbor: C->G, A->R, M->N, W->M
        assertEquals("EGHU9179570", Iso6346.extractBest("ECHU 917957 0").orElseThrow().compact());
        assertFalse(Iso6346.extractBest("ECHU 917957 0").orElseThrow().checkDigitRepaired());

        assertEquals("PGRU4136576", Iso6346.extractBest("PCRU413657 6").orElseThrow().compact());
        assertEquals("MRKU5229117", Iso6346.extractBest("MAKU 5229117").orElseThrow().compact());
        assertEquals("CSNU7993584", Iso6346.extractBest("CSMU 799358 4").orElseThrow().compact());
        assertEquals("MSKU6956025", Iso6346.extractBest("WSKU 6956025").orElseThrow().compact());
        assertEquals("ECBU4571357", Iso6346.extractBest("45 ECBU 1457135 7").orElseThrow().compact());

        // Serial digit misread with a printed 11th check digit must NOT overwrite the 11th digit
        Optional<Iso6346.Match> serialMisread = Iso6346.extractBest("NYKU1632774");
        assertTrue(serialMisread.isPresent());
        assertEquals("NYKU1632774", serialMisread.get().compact());
        assertFalse(serialMisread.get().checksumValid());
    }

    @Test
    void stripsPhoneNumbersMaxGrossStencilsAndTrailingIsoSizeCodes() {
        assertTrue(Iso6346.extractBest("Tel: +31 (0)10-820 0700").isEmpty());
        assertTrue(Iso6346.extractBest("MAX.GROSS 1480KG").isEmpty());
        assertTrue(Iso6346.extractBest("MAXCROSS3.488S").isEmpty());

        // Trailing 45G1 size code must not append '4' as a bogus 11th check digit
        Optional<Iso6346.Match> withSizeStencil = Iso6346.extractBest("OOCU 817116 45G1");
        assertTrue(withSizeStencil.isPresent());
        assertEquals("OOCU8171166", withSizeStencil.get().compact());
        assertTrue(withSizeStencil.get().checkDigitRepaired());
    }
}
