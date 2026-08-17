package com.example.nzreceiptapp.domain.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PageResultTest {

    @Test
    public void constructor_calculatesPageMetadata() {
        PageResult<String> result = new PageResult<>(
                Arrays.asList("receipt-16", "receipt-17"), 2, 15, 31);

        assertEquals(2, result.getItems().size());
        assertEquals(2, result.getCurrentPage());
        assertEquals(15, result.getPageSize());
        assertEquals(31, result.getTotalRecords());
        assertEquals(3, result.getTotalPages());
        assertTrue(result.hasPrevious());
        assertTrue(result.hasNext());
    }

    @Test
    public void constructor_exactPageMultipleDoesNotAddExtraPage() {
        PageResult<String> result = new PageResult<>(
                Collections.singletonList("receipt-16"), 2, 15, 30);

        assertEquals(2, result.getTotalPages());
        assertTrue(result.hasPrevious());
        assertFalse(result.hasNext());
    }

    @Test
    public void constructor_firstPageWithMoreRecordsHasNextOnly() {
        PageResult<String> result = new PageResult<>(
                Collections.singletonList("receipt-1"), 1, 15, 16);

        assertFalse(result.hasPrevious());
        assertTrue(result.hasNext());
    }

    @Test
    public void constructor_zeroRecordsRepresentsPageOneOfOne() {
        PageResult<String> result = new PageResult<>(
                Collections.emptyList(), 1, 15, 0);

        assertEquals(1, result.getCurrentPage());
        assertEquals(1, result.getTotalPages());
        assertFalse(result.hasPrevious());
        assertFalse(result.hasNext());
    }

    @Test
    public void items_areDefensivelyCopiedAndCannotBeModified() {
        List<String> source = new ArrayList<>();
        source.add("receipt-1");
        PageResult<String> result = new PageResult<>(source, 1, 15, 1);

        source.add("receipt-2");

        assertEquals(Collections.singletonList("receipt-1"), result.getItems());
        assertThrows(UnsupportedOperationException.class,
                () -> result.getItems().add("receipt-3"));
    }

    @Test
    public void constructor_rejectsNullItems() {
        assertThrows(NullPointerException.class,
                () -> new PageResult<String>(null, 1, 15, 0));
    }

    @Test
    public void constructor_rejectsPageBelowOne() {
        assertThrows(IllegalArgumentException.class,
                () -> new PageResult<>(Collections.emptyList(), 0, 15, 0));
    }

    @Test
    public void constructor_rejectsNonPositivePageSize() {
        assertThrows(IllegalArgumentException.class,
                () -> new PageResult<>(Collections.emptyList(), 1, 0, 0));
    }

    @Test
    public void constructor_rejectsNegativeTotalRecords() {
        assertThrows(IllegalArgumentException.class,
                () -> new PageResult<>(Collections.emptyList(), 1, 15, -1));
    }

    @Test
    public void constructor_rejectsPageAboveTotalPages() {
        assertThrows(IllegalArgumentException.class,
                () -> new PageResult<>(Collections.emptyList(), 3, 15, 30));
    }
}
