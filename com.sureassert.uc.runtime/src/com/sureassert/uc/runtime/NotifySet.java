/* Copyright 2011 Nathan Dolan. All rights reserved. */

package com.sureassert.uc.runtime;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * HashSet with callbacks
 * 
 * @author Nathan Dolan
 * 
 * @param <E> Entry type
 */
public abstract class NotifySet<E> extends HashSet<E> {

	private static final long serialVersionUID = 1L;

	public NotifySet() {

		super();
	}

	public NotifySet(Collection<? extends E> c) {

		super(c);
	}

	public NotifySet(int initialCapacity, float loadFactor) {

		super(initialCapacity, loadFactor);
	}

	public NotifySet(int initialCapacity) {

		super(initialCapacity);
	}

	@Override
	public boolean add(E entry) {

		boolean retval = super.add(entry);
		if (!retval)
			notifyEntryCollision(entry);
		return retval;
	}

	@Override
	public boolean addAll(Collection<? extends E> entries) {

		boolean retval = super.addAll(entries);

		// optimize
		Collection<? extends E> thisSet = this;
		if (!(entries instanceof Set<?>) || (thisSet.size() > entries.size())) {
			Collection<? extends E> tmp = thisSet;
			thisSet = entries;
			entries = tmp;
		}

		// check for collision
		for (E e : thisSet) {
			if (entries.contains(e))
				notifyEntryCollision(e);
		}

		return retval;
	}

	/**
	 * Callback invoked when an attempt is made to add an entry that already exists in the set.
	 * 
	 * @param entry The entry in collision
	 */
	public abstract void notifyEntryCollision(E entry);
}
