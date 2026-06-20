package com.otilm.ca.connector.ejbca.util;

import com.otilm.api.model.common.NameAndIdDto;
import com.otilm.api.model.common.attribute.v2.content.BaseAttributeContentV2;
import com.otilm.api.model.common.attribute.v2.content.ObjectAttributeContentV2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class LocalAttributeUtilTest {

    @Test
    void convertFromNameAndId_singleEntry_returnsObjectContent() {
        NameAndIdDto dto = new NameAndIdDto(42, "TestProfile");

        List<ObjectAttributeContentV2> result = LocalAttributeUtil.convertFromNameAndId(List.of(dto));

        assertEquals(1, result.size());
        ObjectAttributeContentV2 content = result.get(0);
        assertEquals("TestProfile", content.getReference());
        assertNotNull(content.getData());
        assertEquals(42, ((NameAndIdDto) content.getData()).getId());
        assertEquals("TestProfile", ((NameAndIdDto) content.getData()).getName());
    }

    @Test
    void convertFromNameAndId_emptyList_returnsEmpty() {
        List<ObjectAttributeContentV2> result = LocalAttributeUtil.convertFromNameAndId(List.of());
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void convertFromNameAndId_multipleEntries() {
        List<NameAndIdDto> input = List.of(
                new NameAndIdDto(1, "Alpha"),
                new NameAndIdDto(2, "Beta")
        );

        List<ObjectAttributeContentV2> result = LocalAttributeUtil.convertFromNameAndId(input);

        assertEquals(2, result.size());
        assertEquals("Alpha", result.get(0).getReference());
        assertEquals("Beta", result.get(1).getReference());
    }

    @Test
    void convertFromNameAndIdToBase_singleEntry_returnsBaseContent() {
        NameAndIdDto dto = new NameAndIdDto(10, "SomeCA");

        List<BaseAttributeContentV2<?>> result = LocalAttributeUtil.convertFromNameAndIdToBase(List.of(dto));

        assertEquals(1, result.size());
        BaseAttributeContentV2<?> content = result.get(0);
        assertEquals("SomeCA", content.getReference());
        assertNotNull(content.getData());
    }

    @Test
    void convertFromNameAndIdToBase_emptyList_returnsEmpty() {
        List<BaseAttributeContentV2<?>> result = LocalAttributeUtil.convertFromNameAndIdToBase(List.of());
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void convertFromNameAndIdToBase_multipleEntries_preservesOrder() {
        List<NameAndIdDto> input = List.of(
                new NameAndIdDto(100, "First"),
                new NameAndIdDto(200, "Second"),
                new NameAndIdDto(300, "Third")
        );

        List<BaseAttributeContentV2<?>> result = LocalAttributeUtil.convertFromNameAndIdToBase(input);

        assertEquals(3, result.size());
        assertEquals("First", result.get(0).getReference());
        assertEquals("Second", result.get(1).getReference());
        assertEquals("Third", result.get(2).getReference());
    }
}
