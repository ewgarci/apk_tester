import org.apache.lucene.util.OpenBitSet;

public class AppVector implements java.io.Serializable {
	private static final long serialVersionUID = -28769656158015469L;
	public OpenBitSet ContentVector;
	public OpenBitSet LogicVector;

	public AppVector() {
		super();
	}

	public AppVector(OpenBitSet lv, OpenBitSet cv) {
		this();
		this.LogicVector = lv;
		this.ContentVector = cv;
	}

	public boolean isEmpty() {
		return this.LogicVector.isEmpty() && this.ContentVector.isEmpty();
	}
}