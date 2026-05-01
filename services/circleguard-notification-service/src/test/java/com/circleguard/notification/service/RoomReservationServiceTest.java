package com.circleguard.notification.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import java.util.concurrent.CompletableFuture;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class RoomReservationServiceTest {

    @MockBean
    private JavaMailSender mailSender;

    @Autowired
    private RoomReservationService roomReservationService;

    @Test
    void testCancelReservation() {
        CompletableFuture<Void> future = roomReservationService.cancelReservation("circle-1", "loc-1");
        future.join(); // Wait for completion
        assertThat(future).isCompleted();
    }
}
