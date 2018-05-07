package free.rm.skytube.gui.businessobjects;

/**
 * Represents an item to be used by
 * {@link free.rm.skytube.gui.businessobjects.adapters.MultiSelectListPreferenceAdapter}.
 */
public class MultiSelectListPreferenceItem {

	/** Item's ID */
	public String id;
	/** Item's publicly visible text. */
	public String text;
	/** Is the item checked/selected by the user? */
	public boolean isChecked;

	public MultiSelectListPreferenceItem(String id, String text) {
		this(id, text, true);
	}

	public MultiSelectListPreferenceItem(String id, String text, boolean isChecked) {
		this.id = id;
		this.text = text;
		this.isChecked = isChecked;
	}

}
