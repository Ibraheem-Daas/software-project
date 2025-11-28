# Item Reservation System - New Feature

## Overview
The **Item Reservation System** is a new feature that allows library users to reserve media items that are currently unavailable (all copies checked out). Users are notified when their reserved item becomes available, following a fair first-come-first-served queue system.

## Features

### 1. **Reserve Unavailable Items**
- Users can reserve items when all copies are checked out
- System prevents reserving available items (users should borrow directly instead)
- Users cannot have duplicate active reservations for the same item

### 2. **Queue Management**
- Reservations are processed in chronological order (FIFO - First In, First Out)
- Users can check their position in the reservation queue
- Multiple users can reserve the same item

### 3. **Automatic Fulfillment**
- When an item is returned, the system automatically fulfills the next reservation in the queue
- Fulfilled reservations have a 48-hour pickup window
- Users are notified to collect their reserved item

### 4. **Expiration Handling**
- **ACTIVE** reservations: Automatically expire if the item doesn't become available within the queue
- **FULFILLED** reservations: Expire after 48 hours if the user doesn't pick up the item
- Expired reservations are automatically marked and removed from the queue

### 5. **Cancellation**
- Users can cancel their own reservations at any time
- Cancelled reservations are removed from the queue
- Other users move up in the queue when someone cancels

## Database Schema

```sql
CREATE TABLE reservation (
    reservation_id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    item_id INTEGER NOT NULL,
    reservation_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expiry_date TIMESTAMP NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT fk_reservation_user FOREIGN KEY (user_id) REFERENCES app_user(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_reservation_item FOREIGN KEY (item_id) REFERENCES media_item(item_id) ON DELETE CASCADE
);
```

### Indexes for Performance
- `idx_reservation_user` - Fast user lookup
- `idx_reservation_item` - Fast item lookup  
- `idx_reservation_status` - Status filtering
- `idx_reservation_expiry` - Expiration queries
- `idx_reservation_item_status` - Composite index for queue queries

## Reservation Status Flow

```
ACTIVE → When first created, waiting in queue
   ↓
FULFILLED → Item available, user has 48 hours to pick up
   ↓
[User borrows item - reservation complete]

OR

CANCELLED → User cancelled the reservation
EXPIRED → Time limit exceeded (queue wait or pickup window)
```

## API Reference

### ReservationService Interface

#### Create Reservation
```java
Reservation createReservation(int userId, int itemId)
```
- Creates a new reservation for an unavailable item
- Throws `BusinessException` if item is available or user already has active reservation

#### Cancel Reservation
```java
void cancelReservation(int reservationId, int userId)
```
- Cancels an active reservation
- Users can only cancel their own reservations

#### Fulfill Next Reservation
```java
Reservation fulfillNextReservation(int itemId)
```
- Automatically fulfills the next reservation in queue when item is returned
- Returns the fulfilled reservation or null if no reservations

#### Get User Reservations
```java
List<Reservation> getUserReservations(int userId)
List<Reservation> getActiveUserReservations(int userId)
```
- Retrieves all or only active reservations for a user

#### Get Reservation Queue
```java
List<Reservation> getItemReservationQueue(int itemId)
```
- Gets the ordered list of active reservations for an item

#### Queue Position
```java
int getQueuePosition(int reservationId)
```
- Returns the position in queue (1-based) for a reservation
- Returns -1 if reservation not found or not active

#### Expire Old Reservations
```java
int expireOldReservations()
```
- Marks expired reservations (past expiry_date) as EXPIRED
- Returns count of expired reservations
- Should be run as a scheduled task (e.g., daily cron job)

#### Check Active Reservation
```java
boolean hasActiveReservation(int userId, int itemId)
```
- Checks if user has an active reservation for a specific item

## Integration Points

### With LibraryService
- **returnItem()**: When an item is returned, automatically call `fulfillNextReservation()` to notify next user in queue
- **borrowItem()**: Check if item has active reservations before allowing direct borrowing

### With NotificationService
- Send email when reservation is fulfilled (item available for pickup)
- Send reminder emails for soon-to-expire fulfilled reservations
- Notify users when their reservation expires

### With UI Layer
- Add "Reserve" button for unavailable items
- Show reservation status and queue position
- Display "Reserved for you" message when reservation is fulfilled

## Configuration

```java
// In ReservationServiceImpl
private final int RESERVATION_EXPIRY_HOURS = 48;
```

This controls how long:
- ACTIVE → Max wait time before system review
- FULFILLED → How long user has to pick up the item

## Testing

### Unit Tests
- `ReservationServiceImplTest`: 20+ test cases covering all scenarios
  - Creating reservations
  - Validation (user/item existence, availability checks)
  - Duplicate reservation prevention
  - Cancellation (with ownership verification)
  - Queue fulfillment (FIFO order)
  - Expiration handling
  - Queue position calculation

### Integration Tests (TODO)
- End-to-end reservation workflow with real database
- Concurrent reservation attempts
- Item return with automatic fulfillment
- Expiration batch processing

## Usage Examples

### Example 1: User Reserves Unavailable Book
```java
// All copies of "Java Programming" are checked out
ReservationService reservationService = ...;
Reservation reservation = reservationService.createReservation(userId, itemId);

// User can check their position
int position = reservationService.getQueuePosition(reservation.getReservationId());
System.out.println("You are #" + position + " in the queue");
```

### Example 2: Item Returned, Next User Notified
```java
// User returns book
libraryService.returnItem(loanId, LocalDate.now());

// System automatically fulfills next reservation
Reservation fulfilled = reservationService.fulfillNextReservation(itemId);
if (fulfilled != null) {
    // Send notification to user
    notificationService.sendReservationFulfilledEmail(fulfilled);
}
```

### Example 3: User Cancels Reservation
```java
// User decides to cancel
reservationService.cancelReservation(reservationId, userId);

// Other users automatically move up in queue
```

## Benefits

1. **Fair Access**: First-come-first-served ensures fairness
2. **Improved UX**: Users don't need to constantly check availability
3. **Reduced Workload**: Automated notification and queue management
4. **Data Insights**: Track popular items with long wait lists
5. **Retention**: Users more likely to return when they can reserve items

## Future Enhancements

- **Priority Reservations**: VIP members or urgent requests
- **Hold Duration**: Configurable pickup window per item type
- **Batch Notifications**: Daily digest of fulfilled reservations
- **SMS Notifications**: In addition to email
- **Reservation Limits**: Max active reservations per user
- **Analytics Dashboard**: Visualization of reservation metrics

## Deployment Notes

### Database Migration
1. Run `reservation_table.sql` on production database
2. Verify indexes are created
3. Test with sample data

### Scheduled Tasks
Set up a cron job or scheduled task to expire old reservations:
```bash
# Run daily at 2 AM
0 2 * * * /path/to/app expireReservations
```

### Monitoring
- Track average queue length per item
- Monitor fulfilled→expired conversion rate
- Alert on unusually long queues

---

## Files Created/Modified

### New Files
- `domain/Reservation.java` - Domain model
- `repository/ReservationRepository.java` - Repository interface
- `repository/JdbcReservationRepository.java` - JDBC implementation
- `service/ReservationService.java` - Service interface
- `service/ReservationServiceImpl.java` - Service implementation
- `test/service/ReservationServiceImplTest.java` - Unit tests
- `resources/reservation_table.sql` - Database migration

### Modified Files
- `test/resources/schema.sql` - Added reservation table for tests

---

**Version**: 1.0  
**Date**: November 28, 2025  
**Status**: ✅ Implemented and Tested
