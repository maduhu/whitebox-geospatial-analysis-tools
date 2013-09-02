package photogrammetry.util.model;

import java.io.Serializable;
import java.util.Random;

public class Feature implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -834200343342969100L;

	/**
	 * A PRNG that will be used to determine IDs of new features.
	 */
	private static final Random random = new Random();
	
	/**
	 * This feature's id.
	 */
	public final long id;
	
	/**
	 * Create a new feature with a random id.
	 */
	public Feature() {
		id = random.nextLong();
	}
	
	/**
	 * Create a new feature with a given id.
	 * 
	 * @param id the id to use.
	 */
	public Feature(long id) {
		this.id = id;
	}
	
	public long getId() {
		return id;
	}
	
	@Override
	public int hashCode() {
		// Same as Long.hashCode()
		return (int)(id ^ (id >>> 32));
	}
	
	@Override
	public boolean equals(Object obj) {
		return obj instanceof Feature && ((Feature)obj).id == id;
	}

}
