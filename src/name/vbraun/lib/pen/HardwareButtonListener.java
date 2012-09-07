package name.vbraun.lib.pen;

/**
 * There are two kinds of buttons that we support. One is a button on the pen,
 * where you can read out the button state while the pen is touching the tablet.
 * This is done via the PenEvent classes. The other kind of button is one that
 * you can only use to trigger an action, but not read out continuously. For
 * example, the Galaxy Note series pen has a button that you must not press
 * while writing (stupid, I know). But at least we can use it like a hardware
 * button on the bezel.
 * 
 * @author vbraun
 * 
 */
public interface HardwareButtonListener {

	public enum Type {
		DELETE
	};

	public void onHardwareButtonListener(Type button);

}
