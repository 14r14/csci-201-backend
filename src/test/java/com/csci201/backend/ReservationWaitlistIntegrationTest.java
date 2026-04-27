package com.csci201.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.csci201.backend.dto.BookRoomRequest;
import com.csci201.backend.dto.JoinWaitlistRequest;
import com.csci201.backend.dto.ReservationResponse;
import com.csci201.backend.dto.WaitlistResponse;
import com.csci201.backend.entity.WaitlistEntry;
import com.csci201.backend.exception.RoomNotAvailableException;
import com.csci201.backend.repository.WaitlistEntryRepository;
import com.csci201.backend.service.ReservationService;
import com.csci201.backend.service.WaitlistService;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("h2")
@Transactional
class ReservationWaitlistIntegrationTest {

    @Autowired ReservationService reservationService;
    @Autowired WaitlistService waitlistService;
    @Autowired WaitlistEntryRepository waitlistEntryRepository;

    // Seed user IDs (from V2__seed_data.sql)
    private static final Long ALICE = 1L;
    private static final Long BOB   = 2L;
    private static final Long CAROL = 3L;
    private static final Long DAVE  = 4L;

    // Seed room ID
    private static final Long SAL_101 = 1L;

    private static final Instant START = Instant.parse("2026-05-01T10:00:00Z");
    private static final Instant END   = Instant.parse("2026-05-01T12:00:00Z");

    // ── Booking ──────────────────────────────────────────────────────────────

    @Test
    void bookRoom_success() {
        ReservationResponse res = reservationService.bookRoom(book(ALICE, SAL_101, START, END));

        assertThat(res.getReservationId()).isNotNull();
        assertThat(res.getStatus()).isEqualTo("CONFIRMED");
        assertThat(res.getUserId()).isEqualTo(ALICE);
        assertThat(res.getStartTime()).isEqualTo(START);
        assertThat(res.getEndTime()).isEqualTo(END);
    }

    @Test
    void bookRoom_overlappingSlot_throws() {
        reservationService.bookRoom(book(ALICE, SAL_101, START, END));

        assertThatThrownBy(() -> reservationService.bookRoom(book(BOB, SAL_101, START, END)))
                .isInstanceOf(RoomNotAvailableException.class);
    }

    @Test
    void bookRoom_invalidTimeRange_throws() {
        assertThatThrownBy(() -> reservationService.bookRoom(book(ALICE, SAL_101, END, START)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("startTime must be before endTime");
    }

    // ── Waitlist join ─────────────────────────────────────────────────────────

    @Test
    void joinWaitlist_success() {
        reservationService.bookRoom(book(ALICE, SAL_101, START, END));

        WaitlistResponse res = waitlistService.joinWaitlist(waitlist(BOB, SAL_101, START));

        assertThat(res.getQueuePosition()).isEqualTo(1);
        assertThat(res.getWaitlistCount()).isEqualTo(1);
        assertThat(res.getRoomId()).isEqualTo(SAL_101);
    }

    @Test
    void joinWaitlist_multipleUsers_incrementsPosition() {
        reservationService.bookRoom(book(ALICE, SAL_101, START, END));

        WaitlistResponse bob  = waitlistService.joinWaitlist(waitlist(BOB,  SAL_101, START));
        WaitlistResponse dave = waitlistService.joinWaitlist(waitlist(DAVE, SAL_101, START));

        assertThat(bob.getQueuePosition()).isEqualTo(1);
        assertThat(dave.getQueuePosition()).isEqualTo(2);
    }

    @Test
    void joinWaitlist_noBookingExists_throws() {
        assertThatThrownBy(() -> waitlistService.joinWaitlist(waitlist(BOB, SAL_101, START)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Book it directly");
    }

    @Test
    void joinWaitlist_duplicate_throws() {
        reservationService.bookRoom(book(ALICE, SAL_101, START, END));
        waitlistService.joinWaitlist(waitlist(BOB, SAL_101, START));

        assertThatThrownBy(() -> waitlistService.joinWaitlist(waitlist(BOB, SAL_101, START)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already on the waitlist");
    }

    // ── Cancellation + promotion ──────────────────────────────────────────────

    @Test
    void cancelReservation_setsStatusCancelled() {
        ReservationResponse booking = reservationService.bookRoom(book(ALICE, SAL_101, START, END));

        ReservationResponse cancelled = reservationService.cancelReservation(booking.getReservationId());

        assertThat(cancelled.getStatus()).isEqualTo("CANCELLED");
    }

    @Test
    void cancelReservation_promotesFirstWaitlistUser() {
        ReservationResponse booking = reservationService.bookRoom(book(ALICE, SAL_101, START, END));
        waitlistService.joinWaitlist(waitlist(BOB,  SAL_101, START));  // position 1
        waitlistService.joinWaitlist(waitlist(DAVE, SAL_101, START));  // position 2

        reservationService.cancelReservation(booking.getReservationId());

        // Bob gets a new CONFIRMED reservation
        List<ReservationResponse> bobReservations = reservationService.getReservationsByUser(BOB);
        assertThat(bobReservations).hasSize(1);
        ReservationResponse promoted = bobReservations.get(0);
        assertThat(promoted.getStatus()).isEqualTo("CONFIRMED");
        assertThat(promoted.getStartTime()).isEqualTo(START);
        assertThat(promoted.getEndTime()).isEqualTo(END);
    }

    @Test
    void cancelReservation_removesPromotedUserFromWaitlist() {
        ReservationResponse booking = reservationService.bookRoom(book(ALICE, SAL_101, START, END));
        waitlistService.joinWaitlist(waitlist(BOB,  SAL_101, START));
        waitlistService.joinWaitlist(waitlist(DAVE, SAL_101, START));

        reservationService.cancelReservation(booking.getReservationId());

        // Bob removed, Dave moves to position 1
        List<WaitlistEntry> queue = waitlistEntryRepository
                .findByRoom_RoomIdAndRequestedTimeSlotOrderByQueuePositionAsc(SAL_101, START.toString());
        assertThat(queue).hasSize(1);
        assertThat(queue.get(0).getUser().getUserId()).isEqualTo(DAVE);
        assertThat(queue.get(0).getQueuePosition()).isEqualTo(1);
    }

    @Test
    void cancelReservation_noWaitlist_leavesQueueEmpty() {
        ReservationResponse booking = reservationService.bookRoom(book(ALICE, SAL_101, START, END));

        reservationService.cancelReservation(booking.getReservationId());

        List<WaitlistEntry> queue = waitlistEntryRepository
                .findByRoom_RoomIdAndRequestedTimeSlotOrderByQueuePositionAsc(SAL_101, START.toString());
        assertThat(queue).isEmpty();
    }

    @Test
    void cancelReservation_alreadyCancelled_throws() {
        ReservationResponse booking = reservationService.bookRoom(book(ALICE, SAL_101, START, END));
        reservationService.cancelReservation(booking.getReservationId());

        assertThatThrownBy(() -> reservationService.cancelReservation(booking.getReservationId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already cancelled");
    }

    // ── Leave waitlist + position reordering ─────────────────────────────────

    @Test
    void leaveWaitlist_middle_reordersRemainingPositions() {
        reservationService.bookRoom(book(ALICE, SAL_101, START, END));
        waitlistService.joinWaitlist(waitlist(BOB,   SAL_101, START));  // position 1
        WaitlistResponse carolEntry = waitlistService.joinWaitlist(waitlist(CAROL, SAL_101, START));  // position 2
        waitlistService.joinWaitlist(waitlist(DAVE,  SAL_101, START));  // position 3

        waitlistService.leaveWaitlist(carolEntry.getWaitlistId());  // remove position 2

        List<WaitlistEntry> queue = waitlistEntryRepository
                .findByRoom_RoomIdAndRequestedTimeSlotOrderByQueuePositionAsc(SAL_101, START.toString());
        assertThat(queue).hasSize(2);
        assertThat(queue.get(0).getUser().getUserId()).isEqualTo(BOB);
        assertThat(queue.get(0).getQueuePosition()).isEqualTo(1);
        assertThat(queue.get(1).getUser().getUserId()).isEqualTo(DAVE);
        assertThat(queue.get(1).getQueuePosition()).isEqualTo(2);  // shifted down from 3
    }

    @Test
    void leaveWaitlist_unknownId_throws() {
        assertThatThrownBy(() -> waitlistService.leaveWaitlist(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Waitlist entry not found");
    }

    // ── User-facing queries ───────────────────────────────────────────────────

    @Test
    void getReservationsByUser_returnsAllReservations() {
        reservationService.bookRoom(book(ALICE, SAL_101, START, END));

        List<ReservationResponse> results = reservationService.getReservationsByUser(ALICE);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStatus()).isEqualTo("CONFIRMED");
    }

    @Test
    void getReservationsByUser_unknownUser_throws() {
        assertThatThrownBy(() -> reservationService.getReservationsByUser(999L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void getWaitlistByUser_returnsEntries() {
        reservationService.bookRoom(book(ALICE, SAL_101, START, END));
        waitlistService.joinWaitlist(waitlist(BOB, SAL_101, START));

        List<WaitlistResponse> results = waitlistService.getWaitlistByUser(BOB);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getRoomId()).isEqualTo(SAL_101);
        assertThat(results.get(0).getQueuePosition()).isEqualTo(1);
    }

    @Test
    void getWaitlistByUser_noEntries_returnsEmpty() {
        List<WaitlistResponse> results = waitlistService.getWaitlistByUser(BOB);
        assertThat(results).isEmpty();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private BookRoomRequest book(Long userId, Long roomId, Instant start, Instant end) {
        BookRoomRequest r = new BookRoomRequest();
        r.setUserId(userId);
        r.setRoomId(roomId);
        r.setStartTime(start);
        r.setEndTime(end);
        return r;
    }

    private JoinWaitlistRequest waitlist(Long userId, Long roomId, Instant start) {
        JoinWaitlistRequest r = new JoinWaitlistRequest();
        r.setUserId(userId);
        r.setRoomId(roomId);
        r.setRequestedTimeSlot(start.toString());
        return r;
    }
}
