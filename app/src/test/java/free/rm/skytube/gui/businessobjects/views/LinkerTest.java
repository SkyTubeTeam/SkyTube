package free.rm.skytube.gui.businessobjects.views;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LinkerTest {

    @Test
    void testCleanups() {
        assertEquals("First line of the abstract has a spelling mistake (\"close\" for \"closed\").", Linker.cleanup("First line of the abstract has a spelling mistake (&quot;close&quot; for &quot;closed&quot;)."));
        assertEquals("I'm so glad that time travel is and will always remain impossible.", Linker.cleanup("I&apos;m so glad that time travel is and will always remain impossible."));
        assertEquals("I'm from the future.  Devo explained it.", Linker.cleanup("I&apos;m from the future. &nbsp;Devo explained it."));
    }
}
