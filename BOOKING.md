# Room Booking API

## Endpoint

```
POST /reservations
```

Creates a confirmed reservation for an existing user and room.

## Request Body

```json
{
  "userId": 1,
  "roomId": 2,
  "startTime": "2026-04-20T10:00:00Z",
  "endTime": "2026-04-20T12:00:00Z"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `userId` | Long | Yes | ID of the user making the booking |
| `roomId` | Long | Yes | ID of the room to book |
| `startTime` | ISO 8601 Instant | Yes | Start of the reservation (UTC) |
| `endTime` | ISO 8601 Instant | Yes | End of the reservation (UTC) |

## Response

**201 Created**

```json
{
  "reservationId": 1,
  "userId": 1,
  "roomId": 2,
  "buildingName": "SAL",
  "roomNumber": "102",
  "startTime": "2026-04-20T10:00:00Z",
  "endTime": "2026-04-20T12:00:00Z",
  "status": "CONFIRMED",
  "createdTimestamp": "2026-04-18T20:00:00Z"
}
```

## Error Responses

| Status | Reason |
|--------|--------|
| 400 | `startTime` is not before `endTime`, or a required field is missing |
| 404 | `userId` or `roomId` does not exist |
| 409 | Room already has a `CONFIRMED` reservation overlapping the requested time slot |

## Files Added

| File | Description |
|------|-------------|
| `src/main/java/com/csci201/backend/dto/BookRoomRequest.java` | Request DTO |
| `src/main/java/com/csci201/backend/dto/ReservationResponse.java` | Response DTO |
| `src/main/java/com/csci201/backend/service/ReservationService.java` | Booking logic |
| `src/main/java/com/csci201/backend/controller/ReservationController.java` | REST controller |
| `src/main/java/com/csci201/backend/exception/RoomNotAvailableException.java` | 409 exception |
| `src/main/java/com/csci201/backend/exception/GlobalExceptionHandler.java` | Exception → HTTP status mapping |
| `src/main/resources/flyway/mysql/V2__seed_data.sql` | Seed users and rooms (MySQL) |
| `src/main/resources/flyway/h2/V2__seed_data.sql` | Seed users and rooms (H2/tests) |

## Files Modified

| File | Change |
|------|--------|
| `src/main/java/com/csci201/backend/repository/ReservationRepository.java` | Added `existsOverlappingReservation` JPQL query |

## Seed Data

5 users and 6 rooms are inserted by `V2__seed_data.sql` on first startup.

**Users**

| user_id | user_name | role |
|---------|-----------|------|
| 1 | alice | STUDENT |
| 2 | bob | STUDENT |
| 3 | carol | INSTRUCTOR |
| 4 | dave | STUDENT |
| 5 | admin1 | ADMIN |

**Rooms**

| room_id | building | number | capacity | status |
|---------|----------|--------|----------|--------|
| 1 | SAL | 101 | 20 | AVAILABLE |
| 2 | SAL | 102 | 10 | AVAILABLE |
| 3 | GFS | 201 | 30 | AVAILABLE |
| 4 | GFS | 106 | 6 | AVAILABLE |
| 5 | VKC | 301 | 50 | AVAILABLE |
| 6 | VKC | B10 | 8 | MAINTENANCE |
