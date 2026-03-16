package rs.raf.banka2_bek.payment.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import rs.raf.banka2_bek.client.model.Client;
import rs.raf.banka2_bek.client.repository.ClientRepository;
import rs.raf.banka2_bek.payment.model.PaymentRecipient;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class PaymentRecipientRepositoryTest {

    @Autowired
    private PaymentRecipientRepository paymentRecipientRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Client client1;
    private Client client2;

    @BeforeEach
    void setUp() {
        paymentRecipientRepository.deleteAll();
        clientRepository.deleteAll();

        client1 = Client.builder()
                .firstName("Client")
                .lastName("One")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .email("client1@test.com")
                .phone("+381611111111")
                .password("hashed")
                .saltPassword("salt1")
                .active(true)
                .build();
        client2 = Client.builder()
                .firstName("Client")
                .lastName("Two")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .email("client2@test.com")
                .phone("+381622222222")
                .password("hashed")
                .saltPassword("salt2")
                .active(true)
                .build();
        client1 = clientRepository.save(client1);
        client2 = clientRepository.save(client2);
        entityManager.flush();
    }

    @Test
    void findByClientOrderByCreatedAtDesc_returnsOnlyForClientInDescOrder() {
        PaymentRecipient r1 = PaymentRecipient.builder().client(client1).name("R1").accountNumber("111111111111111111").build();
        PaymentRecipient r2 = PaymentRecipient.builder().client(client1).name("R2").accountNumber("222222222222222222").build();
        PaymentRecipient r3 = PaymentRecipient.builder().client(client2).name("R3").accountNumber("333333333333333333").build();
        paymentRecipientRepository.saveAll(List.of(r1, r2, r3));
        entityManager.flush();
        entityManager.clear();

        List<PaymentRecipient> forClient1 = paymentRecipientRepository.findByClientOrderByCreatedAtDesc(client1);

        assertThat(forClient1).hasSize(2);
        assertThat(forClient1).extracting(PaymentRecipient::getName).containsExactlyInAnyOrder("R1", "R2");

        List<PaymentRecipient> forClient2 = paymentRecipientRepository.findByClientOrderByCreatedAtDesc(client2);
        assertThat(forClient2).hasSize(1);
        assertThat(forClient2.get(0).getName()).isEqualTo("R3");
    }

    @Test
    void findByIdAndClient_returnsOnlyWhenClientMatches() {
        PaymentRecipient recipient = PaymentRecipient.builder().client(client1).name("Rec").accountNumber("123456789012345678").build();
        recipient = paymentRecipientRepository.save(recipient);
        entityManager.flush();
        Long id = recipient.getId();

        Optional<PaymentRecipient> found = paymentRecipientRepository.findByIdAndClient(id, client1);
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Rec");

        Optional<PaymentRecipient> otherClient = paymentRecipientRepository.findByIdAndClient(id, client2);
        assertThat(otherClient).isEmpty();
    }

    @Test
    void existsByIdAndClient_returnsTrueOnlyForOwner() {
        PaymentRecipient recipient = PaymentRecipient.builder().client(client1).name("Rec").accountNumber("123456789012345678").build();
        recipient = paymentRecipientRepository.save(recipient);
        entityManager.flush();
        Long id = recipient.getId();

        assertThat(paymentRecipientRepository.existsByIdAndClient(id, client1)).isTrue();
        assertThat(paymentRecipientRepository.existsByIdAndClient(id, client2)).isFalse();
        assertThat(paymentRecipientRepository.existsByIdAndClient(9999L, client1)).isFalse();
    }
}
