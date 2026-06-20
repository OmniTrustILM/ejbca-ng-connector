package com.otilm.ca.connector.ejbca.util;

import com.otilm.api.model.common.NameAndIdDto;
import com.otilm.api.model.common.attribute.v2.content.BaseAttributeContentV2;
import com.otilm.api.model.common.attribute.v2.content.ObjectAttributeContentV2;

import java.util.ArrayList;
import java.util.List;

public class LocalAttributeUtil {

    private LocalAttributeUtil() {
        // utility class
    }

    public static List<ObjectAttributeContentV2> convertFromNameAndId(List<NameAndIdDto> data) {
        List<ObjectAttributeContentV2> contentList = new ArrayList<>();
        for (NameAndIdDto x : data) {
            ObjectAttributeContentV2 content = new ObjectAttributeContentV2(x.getName(), x);
            contentList.add(content);
        }
        return contentList;
    }

    public static List<BaseAttributeContentV2<?>> convertFromNameAndIdToBase(List<NameAndIdDto> data) {
        List<BaseAttributeContentV2<?>> contentList = new ArrayList<>();
        for (NameAndIdDto x : data) {
            ObjectAttributeContentV2 content = new ObjectAttributeContentV2(x.getName(), x);
            contentList.add(content);
        }
        return contentList;
    }

}
