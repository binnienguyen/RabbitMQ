package vn.vnpay.messageapi.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResponseDto {
    private String requestId;
    private String messageResponse;
    private String dateExpired;
}
