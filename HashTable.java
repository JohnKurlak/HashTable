/**
 * Programmer:		John Kurlak
 * Last Modified:	11.8.2010
 * Purpose:			To implement a generic hash table
 */

// Import
import java.util.Vector;

/**
 * This class represents a generic hash table.
 *
 * @author	John Kurlak
 * @date	11.8.2010
 * @param	<K>	Any generic type representing the key for the hash table; this
 * 				must implement toString() in a unique manner for unique keys
 * @param	<V>	Any generic type representing the values for the hash table
 */
public class HashTable<K, V>
{
	/**
	 * This class represents an entry (or collection of entries) in the hash
	 * table.
	 *
	 * @author	John Kurlak
	 * @param	<K>	Any generic type representing the key for the hash table;
	 * 				this must implement toString() in a unique manner for unique
	 * 				keys
	 * @param	<V>	Any generic type representing the values for the hash table
	 */
	private static class HashEntry<K, V>
	{
		private K key;
		private Vector<V> value = new Vector<V>();
		private boolean isTombstone = false;

		/**
		 * This constructor creates a new hash entry at the given key with the
		 * given value.
		 *
		 * @param	insertKey		The key for the entry
		 * @param	insertValue		The value for the entry
		 */
		private HashEntry(K insertKey, V insertValue)
		{
			key = insertKey;
			value.add(insertValue);
		}

		/**
		 * This method makes a hash entry into a "tombstone" (a deleted entry)
		 */
		public void makeTombstone()
		{
			isTombstone = true;
		}
	}

	private final int[] SIZES = { 1019, 2027, 4079, 8123, 16267, 32503, 65011,
		130027, 260111, 520279, 1040387, 2080763, 4161539, 8323151, 16646323 };
	private int sizeIdx = 0;
	private HashEntry<K, V>[] table;
	private int numEntries = 0;
	private int numFilledSlots = 0;
	private int numProbes = 0;

	/**
	 * This method creates a new hash table of the next size.
	 */
	@SuppressWarnings("unchecked")
	public HashTable()
	{
		table = new HashEntry[SIZES[sizeIdx]];
	}

	/**
	 * This method increases the hash table's size to the next available size.
	 */
	@SuppressWarnings("unchecked")
	private void increaseCapacity()
	{
		// Store a reference to the old table
		HashEntry<K, V>[] oldTable = table;

		// Attempt to resize the table
		try
		{
			// Make a new table full of empty entries
			table = new HashEntry[SIZES[++sizeIdx]];
		}
		// We have too many entries in the hash table: no more sizes left
		catch (ArrayIndexOutOfBoundsException e)
		{
			System.out.println("Too many entries in hash table.  Exiting.");
			System.exit(4);
		}

		for (int i = 0; i < oldTable.length; ++i)
		{
			// If we are at an entry with a key and value
			if (oldTable[i] != null && !oldTable[i].isTombstone)
			{
				// Add every value at that key to the bigger table
				for (V value : oldTable[i].value)
				{
					insert(oldTable[i].key, value);
				}
			}
		}
	}

	/**
	 * This method inserts a value into the hash table with the given key.
	 *
	 * @param	key		The key with which to associate the value
	 * @param	value	The value to insert
	 * @return	True if the insertion succeeded and false if it did not
	 */
	public boolean insert(K key, V value)
	{
		int size = SIZES[sizeIdx];
		int i;
		numProbes = 0;

		// Make sure we haven't filled 70% or more of the entries
		if (numFilledSlots >= 0.7 * size)
		{
			// If we're running out of space, increase the size of our table
			increaseCapacity();
			size = SIZES[sizeIdx];
		}

		// Probe no more iterations than the size of the table
		for (i = 0; i < size; ++i)
		{
			// Compute the next index in the probe sequence
			int index = probe(key, i, size);

			// If the current slot doesn't contain a value
			if (table[index] == null || table[index].isTombstone)
			{
				// Store the given value at the current slot
				table[index] = new HashEntry<K, V>(key, value);
				++numEntries;
				++numFilledSlots;
				numProbes = i;

				return true;
			}
			// If the current slot has a value with the same key as the key we
			// are inserting with
			else if (table[index].key.equals(key) && !table[index].isTombstone)
			{
				// Add the given value to the current slot
				table[index].value.add(value);
				++numEntries;
				numProbes = i;

				return true;
			}
		}

		// Compute the number of probes if probing failed
		numProbes = i - 1;

		// The value wasn't inserted because we couldn't find a place to insert
		// it
		return false;
	}

	/**
	 * This method returns the next probe value.
	 *
	 * @param	key		The key to probe for
	 * @param	i		The current probe index
	 * @param	size	The current size of the hash table
	 * @return	The next probe value
	 */
	private int probe(K key, int i, int size)
	{
		// Use quadratic probing of the form: (i^2 + i) / 2
		return (hash(key) + ((int) (Math.pow(i, 2) + i) >> 2)) % size;
	}

	/**
	 * This method returns the maximum number of probes that were necessary in
	 * the most recent find() call.
	 *
	 * @return	The number of maximum number of probes that were necessary in
	 * 			the most recent find() call
	 */
	public int getNumProbes()
	{
		return numProbes;
	}

	/**
	 * This method finds the value(s) stored at the given key.
	 *
	 * @param	key		The key to find the value(s) for
	 * @return	A vector of values that lie at the given key
	 */
	public Vector<V> find(K key)
	{
		int size = SIZES[sizeIdx];

		// Probe no more iterations than the size of the table
		for (int i = 0; i < size; ++i)
		{
			// Compute the next index in the probe sequence
			int index = probe(key, i, size);

			// If we reach an empty slot, we know the key isn't in the table
			if (table[index] == null)
			{
				return null;
			}
			// If we find the key and it isn't a tombstone
			else if (table[index].key.equals(key) && !table[index].isTombstone)
			{
				// Return the vector of values for that slot
				return table[index].value;
			}
		}

		// If we've been probing for a long time, the key probably isn't in the
		// table
		return null;
	}

	/**
	 * This method deletes all of the values stored at a particular key value.
	 *
	 * @param	key		The key value corresponding to the records that we want
	 * 					to delete
	 * @return	True if the deletion was successful and false if it wasn't
	 */
	public boolean delete(K key)
	{
		int size = SIZES[sizeIdx];

		// Probe no more iterations than the size of the table
		for (int i = 0; i < size; ++i)
		{
			// Compute the next index in the probe sequence
			int index = probe(key, i, size);

			// If we reach an empty slot, we know the key isn't in the table
			if (table[index] == null)
			{
				return false;
			}
			// If we find the key and it isn't a tombstone
			else if (table[index].key.equals(key) && !table[index].isTombstone)
			{
				// Make it a tombstone
				table[index].isTombstone = true;

				return true;
			}
		}

		// If we've been probing for a long time, the key probably isn't in the
		// table
		return false;
	}

	/**
	 * This method hashes a key into an integer using the ELF hash algorithm.
	 *
	 * @pre		The key must implement the toString() method in a way such that
	 * 			it returns distinct strings for distinct keys
	 * @post	The return value will return a balanced distribution of hashes
	 * 			among successive calls with different keys
	 * @param	key		The key to hash
	 * @return	An integer representing the hashed version of the key
	 */
	private int hash(K key)
	{
		String toHash = key.toString();
		int hashValue = 0;

		for (int pos = 0; pos < toHash.length(); ++pos)
		{
			// Compute a hash value for the current letter
			hashValue = (hashValue << 4) + toHash.charAt(pos);
			int highBits = hashValue & 0xF0000000;

			if (highBits != 0)
			{
				hashValue ^= highBits >> 24;
			}

			hashValue &= ~highBits;
		}

		return hashValue;
	}

	/**
	 * This method prints debug information for the hash table to the log.
	 *
	 * @pre		The hash table has values in it
	 * @post	The log file will have debug information about the hash table
	 */
	public void debug()
	{
		float entriesPerSlot = (float) numEntries / (float) numFilledSlots;

		String result = "Format of display is\n";
		result += "Slot number: data record\n\n";
		result += "Current table size:\t\t\t\t\t\t" + table.length + "\n";
		result += "Number of elements in table:\t\t\t" + numEntries + "\n";
		result += "Number of filled slots in table:\t\t" + numFilledSlots + "\n";
		result += "Average number of entries per slot is:\t" + entriesPerSlot + "\n";
		System.out.println(result);

		for (int i = 0; i < table.length; i++)
		{
			// If the current slot has a value in it
			if (table[i] != null && !table[i].isTombstone)
			{
				// Store the key that it stores
				result = "\n" + i + ":\t" + ((i < 100) ? "\t" : "") + "[" + table[i].key.toString() + ", ";

				// Loop through all of the entries at that key
				for (V entry : table[i].value)
				{
					// Store the next value at that key
					result += "(" + entry.toString() + "), ";
				}

				result = result.substring(0, result.length() - 2) + "]";
				System.out.println(result);
			}
		}
	}
}