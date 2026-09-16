package com.KairoLink.controller;

import com.KairoLink.dto.RegistrationRequest;
import com.KairoLink.exception.DuplicateEmailException;
import com.KairoLink.exception.InvalidRegistrationException;
import com.KairoLink.service.UserRegistrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class AuthControllerTest {

    private UserRegistrationService userRegistrationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        userRegistrationService = org.mockito.Mockito.mock(UserRegistrationService.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(userRegistrationService))
                .setValidator(validator)
                .build();
    }

    @Test
    void getRegisterReturnsFormAndRegistrationRequest() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeExists("registrationRequest"));
    }

    @Test
    void getLoginReturnsLoginView() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"));
    }

    @Test
    void postRegisterWithInvalidDtoReturnsFormWithoutPasswords() throws Exception {
        var result = mockMvc.perform(post("/register")
                        .param("name", "")
                        .param("email", "not-an-email")
                        .param("password", "short")
                        .param("confirmPassword", "short"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andReturn();

        RegistrationRequest request = (RegistrationRequest) result.getModelAndView()
                .getModel()
                .get("registrationRequest");
        assertNull(request.getPassword());
        assertNull(request.getConfirmPassword());
        verify(userRegistrationService, never()).register(any());
    }

    @Test
    void postRegisterWithDuplicateEmailAddsEmailErrorAndClearsPasswords() throws Exception {
        doThrow(new DuplicateEmailException("Email already exists"))
                .when(userRegistrationService).register(any(RegistrationRequest.class));

        var result = mockMvc.perform(post("/register")
                        .param("name", "Priya")
                        .param("email", "person@example.com")
                        .param("password", "ValidPass1")
                        .param("confirmPassword", "ValidPass1")
                        .param("role", "RIDER"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andReturn();

        RegistrationRequest request = (RegistrationRequest) result.getModelAndView()
                .getModel()
                .get("registrationRequest");
        assertNull(request.getPassword());
        assertNull(request.getConfirmPassword());
        var bindingResult = result.getModelAndView().getModel()
                .get("org.springframework.validation.BindingResult.registrationRequest");
        assertTrue(bindingResult.toString().contains("email"));
        verify(userRegistrationService).register(any(RegistrationRequest.class));
    }

    @Test
    void postRegisterWithInvalidRegistrationAddsGlobalErrorAndClearsPasswords() throws Exception {
        doThrow(new InvalidRegistrationException("Password and confirmation must match"))
                .when(userRegistrationService).register(any(RegistrationRequest.class));

        var result = mockMvc.perform(post("/register")
                        .param("name", "Priya")
                        .param("email", "person@example.com")
                        .param("password", "ValidPass1")
                        .param("confirmPassword", "ValidPass1")
                        .param("role", "RIDER"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andReturn();

        RegistrationRequest request = (RegistrationRequest) result.getModelAndView()
                .getModel()
                .get("registrationRequest");
        assertNull(request.getPassword());
        assertNull(request.getConfirmPassword());
        var bindingResult = result.getModelAndView().getModel()
                .get("org.springframework.validation.BindingResult.registrationRequest");
        assertTrue(bindingResult.toString().contains("Password and confirmation must match"));
    }

    @Test
    void postRegisterWithValidRegistrationRedirectsAndPassesBoundRequest() throws Exception {
        var result = mockMvc.perform(post("/register")
                        .param("name", "Priya")
                        .param("email", "person@example.com")
                        .param("phone", "9876543210")
                        .param("password", "ValidPass1")
                        .param("confirmPassword", "ValidPass1")
                        .param("role", "RIDER"))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/register?success"))
                .andReturn();

        var requestCaptor = org.mockito.ArgumentCaptor.forClass(RegistrationRequest.class);
        verify(userRegistrationService).register(requestCaptor.capture());
        RegistrationRequest request = requestCaptor.getValue();
        assertEquals("Priya", request.getName());
        assertEquals("person@example.com", request.getEmail());
        assertEquals("9876543210", request.getPhone());
        assertEquals("ValidPass1", request.getPassword());
        assertEquals("ValidPass1", request.getConfirmPassword());
        assertEquals(com.KairoLink.entity.Role.RIDER, request.getRole());
    }
}
