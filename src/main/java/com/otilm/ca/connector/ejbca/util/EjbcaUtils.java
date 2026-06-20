package com.otilm.ca.connector.ejbca.util;

import com.otilm.api.exception.ValidationException;
import com.otilm.api.model.core.authority.EndEntityDto;
import com.otilm.api.model.core.authority.EndEntityExtendedInfoDto;
import com.otilm.api.model.core.authority.EndEntityStatus;
import com.otilm.ca.connector.ejbca.ws.*;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class EjbcaUtils {

    private EjbcaUtils() {
        // utility class
    }

    public static UserMatch prepareUsernameMatch(String username) {
        UserMatch usermatch = new UserMatch();
        usermatch.setMatchwith(MatchWith.MATCH_WITH_USERNAME.getCode());
        usermatch.setMatchtype(MatchType.MATCH_TYPE_EQUALS.getCode());
        usermatch.setMatchvalue(username);
        return usermatch;
    }

    public static UserMatch prepareEndEntityProfileMatch(String endEntityProfileName) {
        UserMatch usermatch = new UserMatch();
        usermatch.setMatchwith(MatchWith.MATCH_WITH_ENDENTITYPROFILE.getCode());
        usermatch.setMatchtype(MatchType.MATCH_TYPE_EQUALS.getCode());
        usermatch.setMatchvalue(endEntityProfileName);
        return usermatch;
    }

    public static EndEntityDto mapToUserDetailDTO(UserDataVOWS userDataVOWS) {
        EndEntityDto userDetailDTO = new EndEntityDto();
        userDetailDTO.setUsername(userDataVOWS.getUsername());
        userDetailDTO.setSubjectDN(userDataVOWS.getSubjectDN());
        userDetailDTO.setEmail(userDataVOWS.getEmail());
        userDetailDTO.setSubjectAltName(userDataVOWS.getSubjectAltName());
        userDetailDTO.setStatus(EndEntityStatus.fromCode(userDataVOWS.getStatus()));
        if (userDataVOWS.getExtendedInformation() != null) {
            List<EndEntityExtendedInfoDto> extendedInformationList = userDataVOWS.getExtendedInformation()
                    .stream()
                    .map(i -> new EndEntityExtendedInfoDto(i.getName(), i.getValue()))
                    .toList();
            userDetailDTO.setExtensionData(extendedInformationList);
        }
        return userDetailDTO;
    }

    public static void setUserExtensions(UserDataVOWS user, String extension) {
        if (StringUtils.isNotBlank(extension)) {
            List<ExtendedInformationWS> ei = new ArrayList<>();
            String[] extensions = extension.split(", *"); // remove spaces after the comma
            for (String data : extensions) {
                String[] extValue = data.split("=", 2); // split the string using = to 2 values

                // Validation of the data
                if (extValue.length != 2) {
                    throw new ValidationException("Invalid extension format: " + data);
                }

                String key = extValue[0].trim();
                String value = extValue[1].trim();

                OidUtils.validateOidFormat(key);

                ExtendedInformationWS eiWs = new ExtendedInformationWS();
                eiWs.setName(key);
                eiWs.setValue(value);
                ei.add(eiWs);
            }
            user.getExtendedInformation().addAll(ei);
        }
    }

}
