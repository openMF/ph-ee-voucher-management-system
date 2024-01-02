package org.mifos.pheevouchermanagementsystem.api.implementation;

import static org.mifos.pheevouchermanagementsystem.util.VoucherManagementEnum.BPMN_NOT_FOUND;
import static org.mifos.pheevouchermanagementsystem.util.VoucherManagementEnum.FAILED_RESPONSE;
import static org.mifos.pheevouchermanagementsystem.util.VoucherManagementEnum.SUCCESS_RESPONSE;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.camunda.zeebe.client.api.command.ClientStatusException;
import java.util.concurrent.ExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.mifos.pheevouchermanagementsystem.api.definition.CreateVoucherApi;
import org.mifos.pheevouchermanagementsystem.data.RequestDTO;
import org.mifos.pheevouchermanagementsystem.data.ResponseDTO;
import org.mifos.pheevouchermanagementsystem.service.CreateVoucherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class CreateVoucherApiController implements CreateVoucherApi {

    @Autowired
    CreateVoucherService createVoucherService;

    @Override
    public ResponseEntity<ResponseDTO> createVouchers(String callbackURL, String programId, String registeringInstitutionId,
            RequestDTO requestBody) throws ExecutionException, InterruptedException, JsonProcessingException {
        try {
            createVoucherService.createVouchers(requestBody, callbackURL, registeringInstitutionId);
        } catch (ClientStatusException e) {
            ResponseDTO responseDTO = new ResponseDTO(BPMN_NOT_FOUND.getValue(), BPMN_NOT_FOUND.getMessage(), requestBody.getRequestID());
            return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).body(responseDTO);
        } catch (Exception e) {
            ResponseDTO responseDTO = new ResponseDTO(FAILED_RESPONSE.getValue(), FAILED_RESPONSE.getMessage(), requestBody.getRequestID());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseDTO);
        }
        ResponseDTO responseDTO = new ResponseDTO(SUCCESS_RESPONSE.getValue(), SUCCESS_RESPONSE.getMessage(), requestBody.getRequestID());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(responseDTO);
    }

}
