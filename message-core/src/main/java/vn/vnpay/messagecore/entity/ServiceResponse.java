package vn.vnpay.messagecore.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ServiceResponse<T> {
    private int code;
    private T data;
}
