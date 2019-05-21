package group.flowbird.paymentservice.client.ClientDTO;

import lombok.Data;

import java.util.Date;

@Data
public class InvoiceDTO {
    private long id;
    private long customerId;
    private String status;
    private double amount;
    private String email;
    private Date paidOn;
    private boolean isPaid;
    private boolean inPaymentReminder;
}
