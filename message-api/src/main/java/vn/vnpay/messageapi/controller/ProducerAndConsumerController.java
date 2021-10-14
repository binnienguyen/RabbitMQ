package vn.vnpay.messageapi.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.vnpay.messageapi.base.ServiceResponse;
import vn.vnpay.messageapi.entity.RequestDto;
import vn.vnpay.messageapi.entity.ResponseDto;
import vn.vnpay.messageapi.entity.SendEntity;
import vn.vnpay.messageapi.service.ProducerAndConsumerService;

@RestController
@RequestMapping("/api")
public class ProducerAndConsumerController {

    @Autowired
    private ProducerAndConsumerService producerAndConsumerService;

    /**
     * @Description: Send message controller
     * @Param: Object send
     */
    @PostMapping("/send")
    public ServiceResponse<ResponseDto> sendMessage(@RequestBody SendEntity sendEntity) {
        return producerAndConsumerService.sendMessage(sendEntity);
    }

    @PostMapping("/check-message-v1")
    public ServiceResponse<ResponseDto> checkMessageV1(@RequestBody RequestDto requestDto){
        return producerAndConsumerService.checkMessageV1(requestDto);
    }

    @PostMapping("/check-message-v2")
    public ServiceResponse<ResponseDto> checkMessageV2(@RequestBody RequestDto requestDto){
        return producerAndConsumerService.checkMessageV2(requestDto);
    }

    @PostMapping("/read")
    public void readMessage(){
        producerAndConsumerService.readMessage();
    }
}
