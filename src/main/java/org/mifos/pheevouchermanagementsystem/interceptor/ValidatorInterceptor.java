package org.mifos.pheevouchermanagementsystem.interceptor;

import static org.mifos.connector.common.exception.PaymentHubError.ExtValidationError;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.mifos.connector.common.channel.dto.PhErrorDTO;
import org.mifos.connector.common.exception.PaymentHubErrorCategory;
import org.mifos.connector.common.validation.ValidatorBuilder;
import org.mifos.pheevouchermanagementsystem.api.implementation.CreateVoucherApiController;
import org.mifos.pheevouchermanagementsystem.util.VoucherValidatorsEnum;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class ValidatorInterceptor implements HandlerInterceptor {

    private static final String resource = "ValidatorInterceptor";
    private static final String callbackURL = "X-CallbackURL";
    private static final String registeringInstitutionId = "X-Registering-Institution-ID";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        log.debug("request at interceptor");

        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;

            if (handlerMethod.getBeanType().equals(CreateVoucherApiController.class)) {

                // Using ValidatorBuilder for header validation
                final ValidatorBuilder validatorBuilder = new ValidatorBuilder();
                validatorBuilder.reset().resource(resource).parameter(callbackURL).value(request.getHeader(callbackURL))
                        .isNullWithFailureCode(VoucherValidatorsEnum.INVALID_CALLBACK_URL);

                validatorBuilder.reset().resource(resource).parameter(registeringInstitutionId)
                        .value(request.getHeader(registeringInstitutionId))
                        .isNullWithFailureCode(VoucherValidatorsEnum.INVALID_REGISTERING_INSTITUTION_ID);

                // If errors exist, set the response and return false
                if (validatorBuilder.hasError()) {
                    validatorBuilder.errorCategory(PaymentHubErrorCategory.Validation.toString())
                            .errorCode(VoucherValidatorsEnum.VOUCHER_HEADER_VALIDATION_ERROR.getCode())
                            .errorDescription(VoucherValidatorsEnum.VOUCHER_HEADER_VALIDATION_ERROR.getMessage())
                            .developerMessage(VoucherValidatorsEnum.VOUCHER_HEADER_VALIDATION_ERROR.getMessage())
                            .defaultUserMessage(VoucherValidatorsEnum.VOUCHER_HEADER_VALIDATION_ERROR.getMessage());

                    PhErrorDTO.PhErrorDTOBuilder phErrorDTOBuilder = new PhErrorDTO.PhErrorDTOBuilder(ExtValidationError.getErrorCode());
                    phErrorDTOBuilder.fromValidatorBuilder(validatorBuilder);

                    // Converting PHErrorDTO in JSON Format
                    ObjectMapper objectMapper = new ObjectMapper();
                    String jsonResponse = objectMapper.writeValueAsString(phErrorDTOBuilder.build());

                    // Setting response status and writing the error message
                    response.setHeader("Content-Type", "application/json");
                    response.setStatus(HttpStatus.BAD_REQUEST.value());
                    response.getWriter().write(jsonResponse);

                    return false;
                }
            }
        }
        return true;
    }
}
