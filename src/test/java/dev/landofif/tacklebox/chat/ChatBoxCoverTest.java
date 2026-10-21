package dev.landofif.tacklebox.chat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/** A box that is not full only covers the bottom lines it draws, so huds above them stay visible. */
class ChatBoxCoverTest {
    @Test
    void twoOfFiveLinesKeepTheBottomEdge() {
        int[] box = {100, 50, 220, ChatBoxes.heightOf(5)};
        int[] c = ChatBoxes.fitToLines(box, 2);
        assertArrayEquals(new int[]{100, box[1] + box[3] - ChatBoxes.heightOf(2), 220, ChatBoxes.heightOf(2)}, c);
    }

    @Test
    void fullBoxCoversAll() {
        int[] box = {100, 50, 220, ChatBoxes.heightOf(5)};
        assertArrayEquals(box, ChatBoxes.fitToLines(box, 5));
        assertArrayEquals(box, ChatBoxes.fitToLines(box, 9));
    }

    @Test
    void emptyBoxCoversNothing() {
        assertArrayEquals(new int[]{100, 50, 0, 0}, ChatBoxes.fitToLines(new int[]{100, 50, 220, 80}, 0));
    }
}
