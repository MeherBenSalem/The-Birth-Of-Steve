# Judge Path

The command is `/tbos showcase` and requires operator level. It creates or
resets the connected Parallax Atrium, Hall of Alignment, Choir of Hours, and Broken Meridian,
gives the Lens, and
moves the invoking player to a safe entrance. The first signature reconstruction
must remain reachable in under 60 seconds.

The original Atrium command and Lens interaction passed an author-confirmed client
run on 2026-07-22. The connected Hall and Choir are covered by automated server
tests but still need their first manual visual/playability pass. The under-60-second target
has not yet been measured with a stopwatch.

## Parallax Atrium

1. Enter a world with operator permissions.
2. Run `/tbos showcase`.
3. The command aligns both rooms to the current chunk, gives the Lens, opens a safe
   entrance, teleports the player inside, and resets both sites to Ruin.
4. Right-click with the Lens near the Memory Anchor to begin the 40-tick reveal.
5. Climb the reconstructed two-wide staircase and pass through the upper doorway.

## Hall of Alignment

1. Move fully into the Hall, use the Lens, and wait for all three dials to
   reconstruct.
2. Turn each dial until its solid glass beam reaches the engraved target. Feedback
   uses geometry and text in addition to lighting and color.
3. Crouch-use the Hall's Memory Anchor at any time before completion to reset all
   three dials to their north marks.
4. Once all three beams lock, use the Lens to return to Ruin and cross the new
   two-wide projected path.

## Choir of Hours

1. Move into the Choir and use the Lens. Do not touch the bells yet.
2. Watch and listen to the repeating four-beat demonstration. Confirm each beat
   has a different location, symbol, pitch, lit bell, memory imprint, and text cue.
3. Use the Lens to return to Ruin. Ring the bells in this order: Sun `[*]`, Crown
   `[^]`, Moon `[)]`, Gate `[#]`.
4. Before solving, deliberately ring a wrong bell twice. Confirm the attempt resets
   without damage and the second failure shows the full symbolic sequence.
5. Crouch-use the Choir's Memory Anchor and confirm the unsolved attempt and failure
   count reset. Then enter the correct sequence and cross the new two-wide path.
6. Save and reload once in Remembered state. Confirm playback resumes from a clean
   imprint state rather than leaving stale symbols behind.

## Broken Meridian

1. Enter the fourth connected room in Ruin and confirm neither chasm has a usable
   crossing. Use the Lens; the first two-wide bridge should exist only in Remembered.
2. Cross to the central island. The Meridian Relay starts on the western engraved
   socket, with one solid luminous power channel identifying its active position.
3. Move the relay once to the center, then crouch-use the Memory Anchor. Confirm the
   relay and channel return to the western socket.
4. Move it twice: west to center, then center to east. Confirm the eastern relay
   lights and the completion text explicitly tells you to return to Ruin.
5. Use the Lens. The first bridge must disappear and the relay must decay into a
   cracked, two-wide second crossing. Cross it to the north exit.
6. Deliberately step into each chasm once. Water should prevent fall damage and the
   ladder on the approach-side lip should return you to the last safe platform;
   it must not provide access to the far side.
7. Save and reload in both unsolved Remembered and solved Ruin states. Confirm the
   relay position, powered channel, and cracked crossing reconcile correctly.

Use `/tbos reset_site`, `/tbos locate`, or the permission-restricted
`/tbos debug transition` for testing.
