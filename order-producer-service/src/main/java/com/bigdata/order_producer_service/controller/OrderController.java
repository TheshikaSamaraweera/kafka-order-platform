package com.bigdata.order_producer_service.controller;




import com.bigdata.order_producer_service.dto.OrderRequest;
import com.bigdata.order_producer_service.service.OrderProducer;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderProducer orderProducer;

    @PostMapping
    public String createOrder(@Valid @RequestBody OrderRequest req) {
        orderProducer.sendOrder(req);
        return "Order sent successfully";
    }

    @PostMapping("/random")
    public String randomOrder() {
        OrderRequest req = new OrderRequest(
                String.valueOf((int)(Math.random()*9000+1000)),
                "Item-" + (int)(Math.random()*5+1),
                Math.round((Math.random()*1000)*100)/100.0
        );
        orderProducer.sendOrder(req);
        return "Random order produced: " + req.orderId();
    }
}

