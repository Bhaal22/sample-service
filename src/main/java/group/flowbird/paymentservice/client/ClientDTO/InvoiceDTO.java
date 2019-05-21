package group.flowbird.paymentservice.client.ClientDTO;

import lombok.Data;

import java.util.Date;

@Data
public class InvoiceDTO {
    private long id;
    private long customerId;
    private String status;
    private long amount;
    private String email;
    private Date paidOn;
}
