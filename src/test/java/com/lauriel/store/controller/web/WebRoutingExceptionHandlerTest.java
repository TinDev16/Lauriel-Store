package com.lauriel.store.controller.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.lauriel.store.dto.response.ErrorResponse;
import com.lauriel.store.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.ModelAndView;

class WebRoutingExceptionHandlerTest {

    private final WebRoutingExceptionHandler handler =
        new WebRoutingExceptionHandler(new GlobalExceptionHandler());

    @Test
    void oversizedWebUploadRendersVietnamesePageWith413() {
        var request = new MockHttpServletRequest("POST", "/admin/products/12");
        var response = new MockHttpServletResponse();
        Object result = handler.oversizedUpload(request, response);
        assertThat(result).isInstanceOf(ModelAndView.class);
        var view = (ModelAndView) result;
        assertThat(view.getViewName()).isEqualTo("error/413");
        assertThat(view.getStatus().value()).isEqualTo(413);
        assertThat(view.getModel().get("message")).asString().contains("20 MB");
        assertThat(response.getContentType()).contains("text/html");
    }

    @Test
    void oversizedApiUploadRemainsJsonWith413() {
        var request = new MockHttpServletRequest("POST", "/api/admin/products");
        Object result = handler.oversizedUpload(
            request,
            new MockHttpServletResponse()
        );
        assertThat(result).isInstanceOf(ResponseEntity.class);
        var response = (ResponseEntity<?>) result;
        assertThat(response.getStatusCode().value()).isEqualTo(413);
        assertThat(response.getBody()).isInstanceOf(ErrorResponse.class);
        assertThat(((ErrorResponse) response.getBody()).message()).contains(
            "20 MB"
        );
    }
}
