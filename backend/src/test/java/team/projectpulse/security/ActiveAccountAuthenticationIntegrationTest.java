package team.projectpulse.security;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import team.projectpulse.AbstractIntegrationTest;
import team.projectpulse.system.StatusCode;
import team.projectpulse.user.PeerEvaluationUser;
import team.projectpulse.user.UserRepository;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Integration tests for active-account authentication")
class ActiveAccountAuthenticationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Value("${api.endpoint.base-url}")
    String baseUrl;

    String studentToken;

    String instructorToken;


    @BeforeEach
    void setUp() throws Exception {
        this.studentToken = login("j.smith@abc.edu");
        this.instructorToken = login("b.gates@abc.edu");
    }

    @Test
    @DisplayName("An inactive student or instructor loses existing bearer access and cannot log in")
    void inactiveAccountsAreRejectedForExistingTokensAndNewLogins() throws Exception {
        this.mockMvc.perform(get(this.baseUrl + "/students/4")
                        .header(HttpHeaders.AUTHORIZATION, this.studentToken))
                .andExpect(status().isOk());
        this.mockMvc.perform(get(this.baseUrl + "/students/4")
                        .header(HttpHeaders.AUTHORIZATION, this.instructorToken))
                .andExpect(status().isOk());

        disableAccount("j.smith@abc.edu");
        disableAccount("b.gates@abc.edu");

        this.mockMvc.perform(get(this.baseUrl + "/students/4")
                        .header(HttpHeaders.AUTHORIZATION, this.studentToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.flag").value(false))
                .andExpect(jsonPath("$.code").value(StatusCode.UNAUTHORIZED));
        this.mockMvc.perform(get(this.baseUrl + "/students/4")
                        .header(HttpHeaders.AUTHORIZATION, this.instructorToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.flag").value(false))
                .andExpect(jsonPath("$.code").value(StatusCode.UNAUTHORIZED));

        assertLoginRejected("j.smith@abc.edu");
        assertLoginRejected("b.gates@abc.edu");
    }

    private String login(String username) throws Exception {
        MvcResult result = this.mockMvc.perform(post(this.baseUrl + "/users/login")
                        .with(httpBasic(username, "123456")))
                .andExpect(status().isOk())
                .andReturn();
        JSONObject json = new JSONObject(result.getResponse().getContentAsString());
        return "Bearer " + json.getJSONObject("data").getString("token");
    }

    private void disableAccount(String username) {
        PeerEvaluationUser user = this.userRepository.findByUsername(username).orElseThrow();
        user.setEnabled(false);
        this.userRepository.saveAndFlush(user);
    }

    private void assertLoginRejected(String username) throws Exception {
        this.mockMvc.perform(post(this.baseUrl + "/users/login")
                        .with(httpBasic(username, "123456")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.flag").value(false))
                .andExpect(jsonPath("$.code").value(StatusCode.UNAUTHORIZED));
    }
}
