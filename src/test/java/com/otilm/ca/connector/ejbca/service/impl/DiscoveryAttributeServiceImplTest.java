package com.otilm.ca.connector.ejbca.service.impl;

import com.otilm.api.exception.ValidationException;
import com.otilm.api.model.client.attribute.RequestAttribute;
import com.otilm.api.model.client.attribute.RequestAttributeV2;
import com.otilm.api.model.common.attribute.common.AttributeType;
import com.otilm.api.model.common.attribute.common.BaseAttribute;
import com.otilm.api.model.common.attribute.common.content.AttributeContentType;
import com.otilm.api.model.common.attribute.v2.DataAttributeV2;
import com.otilm.api.model.common.attribute.v2.GroupAttributeV2;
import com.otilm.api.model.common.attribute.v2.InfoAttributeV2;
import com.otilm.api.model.common.attribute.v2.content.BaseAttributeContentV2;
import com.otilm.api.model.common.attribute.v2.content.IntegerAttributeContentV2;
import com.otilm.api.model.common.attribute.v2.content.ObjectAttributeContentV2;
import com.otilm.api.model.common.attribute.v2.content.StringAttributeContentV2;
import com.otilm.ca.connector.ejbca.dao.AuthorityInstanceRepository;
import com.otilm.ca.connector.ejbca.dao.entity.AuthorityInstance;
import com.otilm.ca.connector.ejbca.dto.AuthorityInstanceNameAndUuidDto;
import com.otilm.ca.connector.ejbca.dto.ejbca.request.SearchCertificateCriteriaRestRequest;
import com.otilm.ca.connector.ejbca.enums.DiscoveryKind;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class DiscoveryAttributeServiceImplTest {

    @Mock
    AuthorityInstanceRepository authorityInstanceRepository;

    @InjectMocks
    DiscoveryAttributeServiceImpl service;

    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    private AuthorityInstance makeInstance(String name, String uuid) {
        AuthorityInstance inst = new AuthorityInstance();
        inst.setName(name);
        inst.setUuid(uuid);
        return inst;
    }

    private void stubRepo(List<AuthorityInstance> instances) {
        given(authorityInstanceRepository.findAll()).willReturn(instances);
    }

    // -------------------------------------------------------------------------
    // getAttributes – EJBCA kind
    // -------------------------------------------------------------------------

    @Test
    void getAttributes_ejbca_returnsThreeAttributes() {
        stubRepo(List.of());
        List<BaseAttribute> attrs = service.getAttributes(DiscoveryKind.EJBCA.name());
        assertEquals(3, attrs.size());
    }

    @Test
    void getAttributes_ejbca_firstAttributeIsInfoType() {
        stubRepo(List.of());
        List<BaseAttribute> attrs = service.getAttributes(DiscoveryKind.EJBCA.name());
        BaseAttribute info = attrs.get(0);
        assertEquals(AttributeType.INFO, info.getType());
        assertEquals("a0bd9e99-7ee5-4e1c-bae1-4321209cf658", info.getUuid());
        assertEquals("info_discoveryProcess", info.getName());
        assertInstanceOf(InfoAttributeV2.class, info);
        InfoAttributeV2 infoV2 = (InfoAttributeV2) info;
        assertEquals(AttributeContentType.TEXT, infoV2.getContentType());
        assertFalse(infoV2.getContent().isEmpty(), "info attribute must have text content");
    }

    @Test
    void getAttributes_ejbca_secondAttributeIsEjbcaInstance() {
        AuthorityInstance inst = makeInstance("Test EJBCA", "aaaa-bbbb");
        stubRepo(List.of(inst));

        List<BaseAttribute> attrs = service.getAttributes(DiscoveryKind.EJBCA.name());
        BaseAttribute ejbcaInst = attrs.get(1);

        assertEquals(AttributeType.DATA, ejbcaInst.getType());
        assertEquals("dce22e96-3335-4181-b90c-c7f887d8d109", ejbcaInst.getUuid());
        assertEquals(DiscoveryAttributeServiceImpl.ATTRIBUTE_EJBCA_INSTANCE, ejbcaInst.getName());

        DataAttributeV2 data = (DataAttributeV2) ejbcaInst;
        assertEquals(AttributeContentType.OBJECT, data.getContentType());
        assertEquals(DiscoveryAttributeServiceImpl.ATTRIBUTE_EJBCA_INSTANCE_LABEL, data.getProperties().getLabel());
        assertTrue(data.getProperties().isRequired());
        assertFalse(data.getProperties().isMultiSelect());
        assertTrue(data.getProperties().isList());
        assertEquals(1, data.getContent().size(), "One instance → one content entry");
    }

    @Test
    void getAttributes_ejbca_secondAttributePopulatesContentFromRepo() {
        AuthorityInstance a = makeInstance("Alpha", "uuid-a");
        AuthorityInstance b = makeInstance("Beta", "uuid-b");
        stubRepo(List.of(a, b));

        List<BaseAttribute> attrs = service.getAttributes(DiscoveryKind.EJBCA.name());
        DataAttributeV2 instAttr = (DataAttributeV2) attrs.get(1);

        assertEquals(2, instAttr.getContent().size());
        ObjectAttributeContentV2 firstContent = (ObjectAttributeContentV2) instAttr.getContent().get(0);
        assertEquals("Alpha", firstContent.getReference());
        AuthorityInstanceNameAndUuidDto dto = (AuthorityInstanceNameAndUuidDto) firstContent.getData();
        assertEquals("Alpha", dto.getName());
        assertEquals("uuid-a", dto.getUuid());
    }

    @Test
    void getAttributes_ejbca_thirdAttributeIsGroupWithCallback() {
        stubRepo(List.of());
        List<BaseAttribute> attrs = service.getAttributes(DiscoveryKind.EJBCA.name());
        BaseAttribute group = attrs.get(2);

        assertEquals(AttributeType.GROUP, group.getType());
        assertEquals("cff2d66d-2f5a-420b-a2ad-44754dac6195", group.getUuid());
        assertEquals(DiscoveryAttributeServiceImpl.ATTRIBUTE_GROUP_DISCOVERY_CONF, group.getName());

        GroupAttributeV2 groupV2 = (GroupAttributeV2) group;
        assertNotNull(groupV2.getAttributeCallback());
        assertEquals("GET", groupV2.getAttributeCallback().getCallbackMethod());
        // context uses {kind} placeholder; the literal kind value is in the mappings
        assertTrue(groupV2.getAttributeCallback().getCallbackContext().contains("{kind}"),
                "callback context should contain the {kind} placeholder");
        // at least one mapping carries the kind value
        boolean hasMappingWithKind = groupV2.getAttributeCallback().getMappings().stream()
                .anyMatch(m -> DiscoveryKind.EJBCA.name().equals(m.getValue()));
        assertTrue(hasMappingWithKind, "a callback mapping should carry the kind value");
        assertFalse(groupV2.getAttributeCallback().getMappings().isEmpty());
    }

    // -------------------------------------------------------------------------
    // getAttributes – EJBCA_SCHEDULE kind
    // -------------------------------------------------------------------------

    @Test
    void getAttributes_ejbcaSchedule_returnsThreeAttributes() {
        stubRepo(List.of());
        List<BaseAttribute> attrs = service.getAttributes(DiscoveryKind.EJBCA_SCHEDULE.name());
        assertEquals(3, attrs.size());
    }

    @Test
    void getAttributes_ejbcaSchedule_groupCallbackMappingContainsScheduleKind() {
        stubRepo(List.of());
        List<BaseAttribute> attrs = service.getAttributes(DiscoveryKind.EJBCA_SCHEDULE.name());
        GroupAttributeV2 group = (GroupAttributeV2) attrs.get(2);
        // the literal EJBCA_SCHEDULE value flows via a mapping, not the context template string
        boolean hasMappingWithKind = group.getAttributeCallback().getMappings().stream()
                .anyMatch(m -> DiscoveryKind.EJBCA_SCHEDULE.name().equals(m.getValue()));
        assertTrue(hasMappingWithKind, "a callback mapping should carry the EJBCA_SCHEDULE kind value");
    }

    // -------------------------------------------------------------------------
    // getAttributes – unsupported kind
    // -------------------------------------------------------------------------

    @Test
    void getAttributes_unsupportedKind_throwsValidationException() {
        assertThrows(ValidationException.class,
                () -> service.getAttributes("UNKNOWN_KIND"));
    }

    // -------------------------------------------------------------------------
    // validateAttributes
    // -------------------------------------------------------------------------

    @Test
    void validateAttributes_nullAttributes_returnsFalse() {
        boolean result = service.validateAttributes(DiscoveryKind.EJBCA.name(), null);
        assertFalse(result);
    }

    @Test
    void validateAttributes_unsupportedKind_throwsValidationException() {
        assertThrows(ValidationException.class,
                () -> service.validateAttributes("BAD_KIND", List.of()));
    }

    @Test
    void validateAttributes_emptyListWithRequiredAttrs_throwsValidationException() {
        stubRepo(List.of());
        // empty list passed while the definition has required attributes → ValidationException
        assertThrows(ValidationException.class,
                () -> service.validateAttributes(DiscoveryKind.EJBCA.name(), List.of()));
    }

    @Test
    void validateAttributes_validEjbcaAttributes_returnsTrue() {
        AuthorityInstance inst = makeInstance("MyEJBCA", "inst-uuid-1234");
        stubRepo(List.of(inst));

        // Build a valid ejbcaInstance attribute (OBJECT, required, list, single-select).
        AuthorityInstanceNameAndUuidDto dto = inst.mapToNameAndUuidDto();
        RequestAttributeV2 instanceAttr = new RequestAttributeV2(
                UUID.fromString("dce22e96-3335-4181-b90c-c7f887d8d109"),
                DiscoveryAttributeServiceImpl.ATTRIBUTE_EJBCA_INSTANCE,
                AttributeContentType.OBJECT,
                List.of(new ObjectAttributeContentV2(dto.getName(), dto))
        );

        List<RequestAttribute> input = List.of(instanceAttr);

        // stubRepo called again inside validateAttributes → getAttributes
        boolean result = service.validateAttributes(DiscoveryKind.EJBCA.name(), input);
        assertTrue(result);
    }

    @Test
    void validateAttributes_nullKindMismatch_throwsValidationException() {
        assertThrows(ValidationException.class,
                () -> service.validateAttributes("NOPE", List.of()));
    }

    // -------------------------------------------------------------------------
    // getInstanceAndKindAttributes – EJBCA kind
    // -------------------------------------------------------------------------

    @Test
    void getInstanceAndKindAttributes_ejbca_returnsFiveAttributes() {
        List<BaseAttributeContentV2<?>> url = List.of(new StringAttributeContentV2("https://ejbca.example.com"));
        List<BaseAttributeContentV2<?>> cas = List.of();
        List<BaseAttributeContentV2<?>> eeProfiles = List.of();

        List<BaseAttribute> attrs = service.getInstanceAndKindAttributes(
                DiscoveryKind.EJBCA.name(), eeProfiles, cas, url);

        assertEquals(5, attrs.size(), "EJBCA kind should add issuedAfter → 5 attributes");
    }

    @Test
    void getInstanceAndKindAttributes_ejbca_firstIsRestApiUrl() {
        List<BaseAttributeContentV2<?>> url = List.of(new StringAttributeContentV2("https://ejbca.example.com"));
        List<BaseAttribute> attrs = service.getInstanceAndKindAttributes(
                DiscoveryKind.EJBCA.name(), List.of(), List.of(), url);

        BaseAttribute first = attrs.get(0);
        assertEquals(DiscoveryAttributeServiceImpl.ATTRIBUTE_EJBCA_RESTAPI_URL, first.getName());
        assertEquals("c5b974dd-e00a-44b6-b9bc-0946e79730a2", first.getUuid());
        DataAttributeV2 data = (DataAttributeV2) first;
        assertEquals(AttributeContentType.STRING, data.getContentType());
        assertEquals(url, data.getContent());
    }

    @Test
    void getInstanceAndKindAttributes_ejbca_secondIsCaAttribute() {
        List<BaseAttribute> attrs = service.getInstanceAndKindAttributes(
                DiscoveryKind.EJBCA.name(), List.of(), List.of(), List.of());

        BaseAttribute ca = attrs.get(1);
        assertEquals(DiscoveryAttributeServiceImpl.ATTRIBUTE_EJBCA_CA, ca.getName());
        assertEquals("ffe7c27a-48e4-41fa-93de-8ddac65fec46", ca.getUuid());
        DataAttributeV2 dataAttr = (DataAttributeV2) ca;
        assertTrue(dataAttr.getProperties().isMultiSelect());
        assertFalse(dataAttr.getProperties().isRequired());
    }

    @Test
    void getInstanceAndKindAttributes_ejbca_thirdIsEndEntityProfile() {
        List<BaseAttribute> attrs = service.getInstanceAndKindAttributes(
                DiscoveryKind.EJBCA.name(), List.of(), List.of(), List.of());

        BaseAttribute eep = attrs.get(2);
        assertEquals(DiscoveryAttributeServiceImpl.ATTRIBUTE_END_ENTITY_PROFILE, eep.getName());
        assertEquals("bbf2d142-f35a-437f-81c7-35c128881fc0", eep.getUuid());
    }

    @Test
    void getInstanceAndKindAttributes_ejbca_fourthIsStatus() {
        List<BaseAttribute> attrs = service.getInstanceAndKindAttributes(
                DiscoveryKind.EJBCA.name(), List.of(), List.of(), List.of());

        BaseAttribute status = attrs.get(3);
        assertEquals(DiscoveryAttributeServiceImpl.ATTRIBUTE_EJBCA_STATUS, status.getName());
        assertEquals("2aba1544-abdf-46a0-ab56-ac79f9163018", status.getUuid());
        DataAttributeV2 statusData = (DataAttributeV2) status;
        // status content must contain all CertificateStatus enum values
        int expectedStatusCount = SearchCertificateCriteriaRestRequest.CertificateStatus.values().length;
        assertEquals(expectedStatusCount, statusData.getContent().size());
    }

    @Test
    void getInstanceAndKindAttributes_ejbca_statusContentMatchesEjbcaSearchStatuses() {
        // The discovery "Certificate status" options must match exactly the status values that
        // EJBCA's REST certificate-search API accepts, a set that is stable across every EJBCA
        // version the connector supports from 7.8 onward.
        Set<String> expected = Set.of(
                "CERT_ACTIVE",
                "CERT_NOTIFIEDABOUTEXPIRATION",
                "CERT_REVOKED",
                "REVOCATION_REASON_UNSPECIFIED",
                "REVOCATION_REASON_KEYCOMPROMISE",
                "REVOCATION_REASON_CACOMPROMISE",
                "REVOCATION_REASON_AFFILIATIONCHANGED",
                "REVOCATION_REASON_SUPERSEDED",
                "REVOCATION_REASON_CESSATIONOFOPERATION",
                "REVOCATION_REASON_CERTIFICATEHOLD",
                "REVOCATION_REASON_REMOVEFROMCRL",
                "REVOCATION_REASON_PRIVILEGESWITHDRAWN",
                "REVOCATION_REASON_AACOMPROMISE"
        );

        List<BaseAttribute> attrs = service.getInstanceAndKindAttributes(
                DiscoveryKind.EJBCA.name(), List.of(), List.of(), List.of());

        DataAttributeV2 statusData = (DataAttributeV2) attrs.stream()
                .filter(a -> DiscoveryAttributeServiceImpl.ATTRIBUTE_EJBCA_STATUS.equals(a.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("status attribute not found"));
        List<String> offered = statusData.getContent().stream()
                .map(c -> (String) c.getData())
                .toList();

        // No duplicates in the offered options.
        assertEquals(Set.copyOf(offered).size(), offered.size(),
                "status options must not contain duplicates: " + offered);
        // Exactly the EJBCA-accepted set — nothing missing, nothing extra.
        assertEquals(expected, Set.copyOf(offered));
    }

    @Test
    void getInstanceAndKindAttributes_ejbca_fifthIsIssuedAfter() {
        List<BaseAttribute> attrs = service.getInstanceAndKindAttributes(
                DiscoveryKind.EJBCA.name(), List.of(), List.of(), List.of());

        BaseAttribute last = attrs.get(4);
        assertEquals(DiscoveryAttributeServiceImpl.ATTRIBUTE_EJBCA_ISSUED_AFTER, last.getName());
        assertEquals("4954adc0-47f0-442d-9347-f270d9ac0074", last.getUuid());
        DataAttributeV2 issuedAfterData = (DataAttributeV2) last;
        assertEquals(AttributeContentType.DATETIME, issuedAfterData.getContentType());
        assertFalse(issuedAfterData.getProperties().isRequired());
    }

    // -------------------------------------------------------------------------
    // getInstanceAndKindAttributes – EJBCA_SCHEDULE kind
    // -------------------------------------------------------------------------

    @Test
    void getInstanceAndKindAttributes_ejbcaSchedule_returnsFiveAttributes() {
        List<BaseAttribute> attrs = service.getInstanceAndKindAttributes(
                DiscoveryKind.EJBCA_SCHEDULE.name(), List.of(), List.of(), List.of());

        assertEquals(5, attrs.size(), "EJBCA_SCHEDULE should add issuedDaysBefore → 5 attributes");
    }

    @Test
    void getInstanceAndKindAttributes_ejbcaSchedule_fifthIsIssuedDaysBefore() {
        List<BaseAttribute> attrs = service.getInstanceAndKindAttributes(
                DiscoveryKind.EJBCA_SCHEDULE.name(), List.of(), List.of(), List.of());

        BaseAttribute last = attrs.get(4);
        assertEquals(DiscoveryAttributeServiceImpl.ATTRIBUTE_ISSUED_DAYS_BEFORE, last.getName());
        assertEquals("4a92a6c5-38c0-4ebf-8297-594d39572c9c", last.getUuid());
        DataAttributeV2 daysData = (DataAttributeV2) last;
        assertEquals(AttributeContentType.INTEGER, daysData.getContentType());
        assertTrue(daysData.getProperties().isRequired());
        assertEquals(1, daysData.getContent().size());
        IntegerAttributeContentV2 defaultDays = (IntegerAttributeContentV2) daysData.getContent().get(0);
        assertEquals(5, defaultDays.getData());
    }

    @Test
    void getInstanceAndKindAttributes_ejbcaSchedule_doesNotContainIssuedAfter() {
        List<BaseAttribute> attrs = service.getInstanceAndKindAttributes(
                DiscoveryKind.EJBCA_SCHEDULE.name(), List.of(), List.of(), List.of());

        boolean hasIssuedAfter = attrs.stream()
                .anyMatch(a -> DiscoveryAttributeServiceImpl.ATTRIBUTE_EJBCA_ISSUED_AFTER.equals(a.getName()));
        assertFalse(hasIssuedAfter, "EJBCA_SCHEDULE must not include issuedAfter");
    }

    @Test
    void getInstanceAndKindAttributes_ejbca_doesNotContainIssuedDaysBefore() {
        List<BaseAttribute> attrs = service.getInstanceAndKindAttributes(
                DiscoveryKind.EJBCA.name(), List.of(), List.of(), List.of());

        boolean hasDaysBefore = attrs.stream()
                .anyMatch(a -> DiscoveryAttributeServiceImpl.ATTRIBUTE_ISSUED_DAYS_BEFORE.equals(a.getName()));
        assertFalse(hasDaysBefore, "EJBCA must not include issuedDaysBefore");
    }

    // -------------------------------------------------------------------------
    // getInstanceAndKindAttributes – content injection
    // -------------------------------------------------------------------------

    @Test
    void getInstanceAndKindAttributes_urlContentIsInjected() {
        List<BaseAttributeContentV2<?>> urlContent = new ArrayList<>();
        urlContent.add(new StringAttributeContentV2("https://my-ejbca:8443"));

        List<BaseAttribute> attrs = service.getInstanceAndKindAttributes(
                DiscoveryKind.EJBCA.name(), List.of(), List.of(), urlContent);

        DataAttributeV2 urlAttr = (DataAttributeV2) attrs.get(0);
        assertEquals(1, urlAttr.getContent().size());
        StringAttributeContentV2 urlContent0 = (StringAttributeContentV2) urlAttr.getContent().get(0);
        assertEquals("https://my-ejbca:8443", urlContent0.getData());
    }

    @Test
    void getInstanceAndKindAttributes_caContentIsInjected() {
        AuthorityInstanceNameAndUuidDto caDto = new AuthorityInstanceNameAndUuidDto("MyCA", "ca-uuid");
        List<BaseAttributeContentV2<?>> casContent = List.of(new ObjectAttributeContentV2("MyCA", caDto));

        List<BaseAttribute> attrs = service.getInstanceAndKindAttributes(
                DiscoveryKind.EJBCA.name(), List.of(), casContent, List.of());

        DataAttributeV2 caAttr = (DataAttributeV2) attrs.get(1);
        assertEquals(1, caAttr.getContent().size());
    }

    @Test
    void getInstanceAndKindAttributes_eeProfileContentIsInjected() {
        AuthorityInstanceNameAndUuidDto eeDto = new AuthorityInstanceNameAndUuidDto("MyProfile", "prof-uuid");
        List<BaseAttributeContentV2<?>> eeContent = List.of(new ObjectAttributeContentV2("MyProfile", eeDto));

        List<BaseAttribute> attrs = service.getInstanceAndKindAttributes(
                DiscoveryKind.EJBCA.name(), eeContent, List.of(), List.of());

        DataAttributeV2 eeAttr = (DataAttributeV2) attrs.get(2);
        assertEquals(1, eeAttr.getContent().size());
    }

    // -------------------------------------------------------------------------
    // getAttributes – empty repo → empty instance content
    // -------------------------------------------------------------------------

    @Test
    void getAttributes_emptyRepo_instanceAttributeHasEmptyContent() {
        stubRepo(List.of());
        List<BaseAttribute> attrs = service.getAttributes(DiscoveryKind.EJBCA.name());
        DataAttributeV2 instanceAttr = (DataAttributeV2) attrs.get(1);
        assertTrue(instanceAttr.getContent().isEmpty());
    }
}
