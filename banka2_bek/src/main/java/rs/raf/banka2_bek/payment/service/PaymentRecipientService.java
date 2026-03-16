package rs.raf.banka2_bek.payment.service;

import rs.raf.banka2_bek.payment.dto.CreatePaymentRecipientRequestDto;
import rs.raf.banka2_bek.payment.dto.PaymentRecipientResponseDto;
import rs.raf.banka2_bek.payment.dto.UpdatePaymentRecipientRequestDto;

import java.util.List;

/**
 * Service for payment recipient CRUD. All operations are scoped to the current client.
 */
public interface PaymentRecipientService {

    /**
     * Returns all payment recipients for the client identified by the given email.
     */
    List<PaymentRecipientResponseDto> getPaymentRecipients(String clientEmail);

    /**
     * Creates a new payment recipient for the client identified by the given email.
     */
    PaymentRecipientResponseDto createPaymentRecipient(CreatePaymentRecipientRequestDto request, String clientEmail);

    /**
     * Updates an existing payment recipient by ID if it belongs to the client.
     */
    PaymentRecipientResponseDto updatePaymentRecipient(Long id, UpdatePaymentRecipientRequestDto request, String clientEmail);

    /**
     * Deletes a payment recipient by ID if it belongs to the client.
     */
    void deletePaymentRecipient(Long id, String clientEmail);
}
