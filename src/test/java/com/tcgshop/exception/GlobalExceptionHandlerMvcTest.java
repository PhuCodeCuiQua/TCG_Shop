package com.tcgshop.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.web.csrf.DefaultCsrfToken;

@WebMvcTest(controllers = GlobalExceptionHandlerTestController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class GlobalExceptionHandlerMvcTest {
	@Autowired
	private MockMvc mockMvc;

	@Test
	void apiExceptionsReturnExpectedJsonStatusAndSafeMessages() throws Exception {
		assertApiError("/api/test/not-found", 404, "Not Found");
		assertApiError("/api/test/forbidden", 403, "Forbidden");
		assertApiError("/api/test/access-denied", 403, "Forbidden");
		assertApiError("/api/test/bad-request", 400, "Bad Request");
		assertApiError("/api/test/conflict", 409, "Conflict");
		assertApiError("/api/test/business", 422, "Unprocessable Entity");
		assertApiError("/api/test/optimistic-lock", 409, "Conflict");

		mockMvc.perform(get("/api/test/unexpected").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.timestamp").exists())
				.andExpect(jsonPath("$.status").value(500))
				.andExpect(jsonPath("$.error").value("Internal Server Error"))
				.andExpect(jsonPath("$.message").value("Đã xảy ra lỗi hệ thống. Vui lòng thử lại sau."))
				.andExpect(jsonPath("$.path").value("/api/test/unexpected"))
				.andExpect(jsonPath("$.traceId").isNotEmpty())
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
						.string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("IllegalStateException"))))
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
						.string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("internal stack detail"))));
	}

	@Test
	void validationErrorsContainFieldAndAnnotationMessage() throws Exception {
		mockMvc.perform(post("/api/test/validate")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.content("{\"name\":\" \"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.fieldErrors[0].field").value("name"))
				.andExpect(jsonPath("$.fieldErrors[0].message").value("Tên không được để trống"));
	}

	@Test
	void webGetExceptionsRenderStatusSpecificViews() throws Exception {
		mockMvc.perform(get("/test/not-found").requestAttr("_csrf",
				new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "test-token")))
				.andExpect(status().isNotFound()).andExpect(view().name("error/404"));
		mockMvc.perform(get("/test/forbidden").requestAttr("_csrf",
				new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "test-token")))
				.andExpect(status().isForbidden()).andExpect(view().name("error/403"));
		mockMvc.perform(get("/test/access-denied").requestAttr("_csrf",
				new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "test-token")))
				.andExpect(status().isForbidden()).andExpect(view().name("error/403"));
		mockMvc.perform(get("/test/unexpected").requestAttr("_csrf",
				new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "test-token")))
				.andExpect(status().isInternalServerError()).andExpect(view().name("error/500"));
	}

	@Test
	void webPostBusinessAndBadRequestRedirectWithErrorFlash() throws Exception {
		mockMvc.perform(post("/test/business").header("Referer", "http://localhost/source"))
				.andExpect(status().is3xxRedirection())
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl("/source"))
				.andExpect(flash().attribute("errorMessage", "Không thể thực hiện nghiệp vụ"));
		mockMvc.perform(post("/test/bad-request").header("Referer", "http://localhost/source"))
				.andExpect(status().is3xxRedirection())
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl("/source"))
				.andExpect(flash().attribute("errorMessage", "Dữ liệu không hợp lệ"));
	}

	@Test
	void unknownUrlAndAccessDeniedAreNotReportedAsServerErrors() throws Exception {
		mockMvc.perform(get("/api/test/does-not-exist").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404));
		mockMvc.perform(get("/api/test/access-denied").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.status").value(403));
	}

	private void assertApiError(String path, int expectedStatus, String expectedError) throws Exception {
		mockMvc.perform(get(path).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().is(expectedStatus))
				.andExpect(jsonPath("$.timestamp").exists())
				.andExpect(jsonPath("$.status").value(expectedStatus))
				.andExpect(jsonPath("$.error").value(expectedError))
				.andExpect(jsonPath("$.message").isNotEmpty())
				.andExpect(jsonPath("$.path").value(path));
	}
}
