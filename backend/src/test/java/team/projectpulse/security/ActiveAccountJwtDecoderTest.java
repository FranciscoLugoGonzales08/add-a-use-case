package team.projectpulse.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import team.projectpulse.user.PeerEvaluationUser;
import team.projectpulse.user.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ActiveAccountJwtDecoderTest {

    @Mock
    JwtDecoder decoder;

    @Mock
    UserRepository userRepository;

    @Mock
    PeerEvaluationUser user;

    @Test
    void acceptsTokenForAnActiveAccount() {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "RS256").claim("userId", 42).build();
        given(this.decoder.decode("token")).willReturn(jwt);
        given(this.userRepository.findById(42)).willReturn(Optional.of(this.user));
        given(this.user.isEnabled()).willReturn(true);

        JwtDecoder activeAccountDecoder = SecurityConfiguration.withActiveAccountValidation(
                this.decoder, this.userRepository);

        assertThat(activeAccountDecoder.decode("token")).isSameAs(jwt);
    }

    @Test
    void rejectsTokenForAnInactiveAccount() {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "RS256").claim("userId", 42).build();
        given(this.decoder.decode("token")).willReturn(jwt);
        given(this.userRepository.findById(42)).willReturn(Optional.of(this.user));
        given(this.user.isEnabled()).willReturn(false);

        JwtDecoder activeAccountDecoder = SecurityConfiguration.withActiveAccountValidation(
                this.decoder, this.userRepository);

        assertThatThrownBy(() -> activeAccountDecoder.decode("token"))
                .isInstanceOf(JwtException.class)
                .hasMessage("The account associated with this token is not active.");
    }

    @Test
    void rejectsTokenWhenItsAccountNoLongerExists() {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "RS256").claim("userId", 42).build();
        given(this.decoder.decode("token")).willReturn(jwt);
        given(this.userRepository.findById(42)).willReturn(Optional.empty());

        JwtDecoder activeAccountDecoder = SecurityConfiguration.withActiveAccountValidation(
                this.decoder, this.userRepository);

        assertThatThrownBy(() -> activeAccountDecoder.decode("token"))
                .isInstanceOf(JwtException.class)
                .hasMessage("The account associated with this token is not active.");
    }

    @Test
    void rejectsTokenWithoutAnAccountId() {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "RS256").claim("sub", "user").build();
        given(this.decoder.decode("token")).willReturn(jwt);

        JwtDecoder activeAccountDecoder = SecurityConfiguration.withActiveAccountValidation(
                this.decoder, this.userRepository);

        assertThatThrownBy(() -> activeAccountDecoder.decode("token"))
                .isInstanceOf(JwtException.class)
                .hasMessage("The account associated with this token is not active.");
    }
}
