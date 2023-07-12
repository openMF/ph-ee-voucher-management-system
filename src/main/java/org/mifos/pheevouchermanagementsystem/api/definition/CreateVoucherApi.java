package org.mifos.pheevouchermanagementsystem.api.definition;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.mifos.pheevouchermanagementsystem.data.RequestDTO;
import org.mifos.pheevouchermanagementsystem.data.ResponseDTO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.concurrent.ExecutionException;

public interface CreateVoucherApi {
    @PostMapping("/vouchers")
    ResponseDTO createVouchers(@RequestHeader(value="X-CallbackURL") String callbackURL,
                                   @RequestBody RequestDTO requestBody) throws ExecutionException, InterruptedException, JsonProcessingException;
}
