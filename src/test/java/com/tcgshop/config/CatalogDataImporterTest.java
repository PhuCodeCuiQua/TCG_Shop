package com.tcgshop.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcgshop.dto.response.CatalogImportResponse;
import com.tcgshop.service.CatalogImportService;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CatalogDataImporterTest {
    @Test
    void scansEverySeedJsonAndContinuesAfterFailedItem() throws Exception {
        ObjectMapper mapper = spy(new ObjectMapper());
        var root = mapper.readTree("""
                {"games":[], "series":[], "cards":[], "sealedProducts":[], "supplies":[
                  {"name":"Broken"}, {"name":"Valid"}]}
                """);
        doReturn(root).when(mapper).readTree(any(InputStream.class));
        CatalogImportService service = mock(CatalogImportService.class);
        int seedFileCount = new PathMatchingResourcePatternResolver().getResources("classpath*:seed/*.json").length;
        assertTrue(seedFileCount > 0);
        AtomicInteger calls = new AtomicInteger();
        when(service.importItem(any())).thenAnswer(invocation -> calls.incrementAndGet() % 2 == 1
                ? throwInvalidItem() : new CatalogImportResponse(1, 1));
        assertDoesNotThrow(() -> new CatalogDataImporter(mapper, service).run());
        verify(service, times(seedFileCount * 2)).importItem(any());
    }

    private CatalogImportResponse throwInvalidItem() {
        throw new IllegalArgumentException("Invalid item");
    }

    @Test
    void importerRequiresExplicitConfiguration() {
        var condition = CatalogDataImporter.class.getAnnotation(ConditionalOnProperty.class);
        assertEquals("app.catalog-import.enabled", condition.name()[0]);
        assertEquals("true", condition.havingValue());
        assertFalse(condition.matchIfMissing());
    }
}
