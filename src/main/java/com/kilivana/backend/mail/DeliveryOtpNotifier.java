package com.kilivana.backend.mail;

import com.kilivana.backend.admin.repository.UserRepository;
import com.kilivana.backend.ecommerce.repository.OrderRepository;
import com.kilivana.backend.logistics.entity.LogisticsJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Tells the buyer the code their driver will ask for at handover. Kept separate from
 * LogisticsService so the delivery domain does not depend on a mail provider.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryOtpNotifier {

    private final SendGridMailService mailService;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    /**
     * Sends the code to the buyer named on the job's order. A missing order, buyer or
     * address is logged rather than thrown: the code is also emailed and held nowhere else,
     * so a failed notification must not block the job from being created.
     *
     * The plaintext code is passed in rather than read from the job, because the job only
     * ever stores a hash of it.
     */
    public void sendOtpToBuyer(LogisticsJob job, String otp) {
        if (job == null || otp == null) {
            return;
        }

        orderRepository.findById(job.getOrderId())
                .map(order -> userRepository.findById(order.getBuyerId()).orElse(null))
                .filter(buyer -> buyer != null && buyer.getEmail() != null)
                .ifPresentOrElse(
                        buyer -> mailService.send(buyer.getEmail(),
                                "Your Kilivana delivery code",
                                buildBody(job, otp, buyer.getName())),
                        () -> log.warn("No buyer email available for job {}; delivery code not sent",
                                job.getId()));
    }

    private String buildBody(LogisticsJob job, String otp, String buyerName) {
        return "Hi " + buyerName + ",\n\n"
                + "Your delivery is on its way.\n\n"
                + "Delivery code: " + otp + "\n"
                + "From: " + job.getPickupAddress() + "\n"
                + "To: " + job.getDestinationAddress() + "\n\n"
                + "Please share this code with your driver at handover. They cannot complete "
                + "the delivery without it.\n\n"
                + "If someone asked you for this code unexpectedly, do not share it and "
                + "contact Kilivana support.\n";
    }
}