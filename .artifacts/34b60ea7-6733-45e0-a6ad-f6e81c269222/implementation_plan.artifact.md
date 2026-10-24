# SWEET Messenger v0.01.1.9.2 Stabilization & Features

This plan addresses all reported issues and adds the missing functionality for a complete WhatsApp/Telegram style experience.

## Open Questions
- None.

## Proposed Changes

### `ChatRepository.kt`
- **Fix:** Update `getCanonicalChatId` to prevent recursive ID appending (which causes the very long names and duplicate chat list entries).
- **Fix:** Add `msgId`, `fileName`, `fileSize`, `isBeam` to the `sendMessage` payload and interface to fix compile errors.
- **Fix:** Implement strict deduplication using `msgId` in `handleIncomingPayload`.
- **Feature:** Implement basic SharedPreferences persistence for recent conversations, known peers, pinned chats, and custom aliases so users can continue chatting without re-discovering.

### `ChatViewModel.kt` & `ChatConversation.kt`
- **Fix:** Fetch the correct `displayName` (using alias if available) to show in the Conversation header instead of "SWEET Node #...".
- **Fix:** Pass correct parameters to `chatRepository.sendMessage`.

### `ConversationScreen.kt`
- **Fix:** Remove compile errors (`fileName` not found).
- **Feature:** Use `LazyListState` and `LaunchedEffect(messages.size)` to automatically scroll to the bottom when a new message is sent/received.
- **Feature:** Move emoji reactions outside and to the bottom-right/left of the chat bubble.
- **Feature:** Implement real device file picker (`ActivityResultContracts.GetContent()`) for the SWEET Beam attachment sheet.
- **Feature:** Add a profile menu click on the TopBar to view the peer's profile/edit their name.

### `ChatListScreen.kt`
- **Fix:** Ensure `Modifier.clickable` is properly imported to fix compile errors.

### `ProfileSettingsScreen.kt` & `UserPreferencesRepository.kt`
- **Feature:** Save the selected profile picture URI to DataStore so it persists across app restarts.
- **Feature:** Add a 2-second "Saved!" toast pop-up when changes are successfully saved.

## Verification Plan
1. Compile and build successfully on both uLesson and Samsung devices.
2. Run the John (uLesson) and Doe (Samsung) simulation.
3. Update names/profile pictures and verify persistence.
4. Discover and chat. Verify single chat instance in the list.
5. Edit friend's name to "Best friend".
6. Overwhelm with 15 questions and reply to them individually.
7. Verify auto-scroll, external reactions, and real file picker.