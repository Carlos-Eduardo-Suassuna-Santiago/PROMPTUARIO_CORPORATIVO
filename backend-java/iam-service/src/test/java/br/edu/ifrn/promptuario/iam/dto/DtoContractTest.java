package br.edu.ifrn.promptuario.iam.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DtoContractTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void rejectsInvalidLoginPayload() {
        LoginRequestDTO request = new LoginRequestDTO("invalid-email", "", null);

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactlyInAnyOrder("email", "password");
    }

    @Test
    void serializesTokenResponseUsingFrontendFieldNames() throws Exception {
        TokenResponseDTO response = new TokenResponseDTO("access", "refresh", 1_800, null);

        String json = objectMapper.writeValueAsString(response);

        assertThat(json)
                .contains("\"access_token\":\"access\"")
                .contains("\"refresh_token\":\"refresh\"")
                .contains("\"token_type\":\"bearer\"")
                .contains("\"expires_in\":1800")
                .doesNotContain("accessToken", "refreshToken", "tokenType", "expiresIn");
    }
}
