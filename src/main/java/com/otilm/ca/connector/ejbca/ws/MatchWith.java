package com.otilm.ca.connector.ejbca.ws;

import java.util.Arrays;

public enum MatchWith {

    MATCH_WITH_CA(5),
    MATCH_WITH_CERTIFICATEPROFILE(4),
    MATCH_WITH_COMMONNAME(101),
    MATCH_WITH_COUNTRY(112),
    MATCH_WITH_DN(7),
    MATCH_WITH_DNSERIALNUMBER(102),
    MATCH_WITH_DOMAINCOMPONENT(111),
    MATCH_WITH_EMAIL(1),
    MATCH_WITH_ENDENTITYPROFILE(3),
    MATCH_WITH_GIVENNAME(103),
    MATCH_WITH_INITIALS(104),
    MATCH_WITH_LOCALITY(109),
    MATCH_WITH_ORGANIZATION(108),
    MATCH_WITH_ORGANIZATIONALUNIT(107),
    MATCH_WITH_STATEORPROVINCE(110),
    MATCH_WITH_STATUS(2),
    MATCH_WITH_SURNAME(105),
    MATCH_WITH_TITLE(106),
    MATCH_WITH_TOKEN(6),
    MATCH_WITH_UID(100),
    MATCH_WITH_USERNAME(0)	;

    private final int code;

    private MatchWith(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static MatchWith fromCode(int code) {
        return Arrays.stream(values())
                .filter(e -> e.code == code)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(String.format("Unsupported type %s.", code)));
    }
}
