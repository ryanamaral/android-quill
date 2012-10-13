package com.write.Quill.sync;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.UUID;

import junit.framework.Assert;

import com.write.Quill.R;
import com.write.Quill.data.Bookshelf;
import com.write.Quill.data.Bookshelf.BookPreview;
import com.write.Quill.sync.SyncData.SyncItem.State;

import android.content.Context;
import android.database.DataSetObserver;
import android.text.format.Time;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

/**
 * The sync data for all books
 * @author vbraun
 */
public class SyncData {
	private final static String TAG = "SyncData";

	public static class SyncItem {
		protected enum State { 
			IN_SYNC, LOCAL_ONLY, LOCAL_IS_NEWER, REMOTE_IS_NEWER, CONFLICT 
		};
		
		protected State state;
		
		protected boolean local = false;
		protected boolean remote = false;
		protected String localTitle, remoteTitle;
		protected UUID uuid;
		
		// mtime of the book at last sync
		
		protected Time lastSync;
		// the last-modified time
		protected Time localTime, remoteTime;
	
		public void setLocal(BookPreview book) {
			local = true;
			Assert.assertTrue(this.uuid == null || this.uuid.equals(uuid));
			this.uuid = book.getUUID();
			localTitle = book.getTitle();
			localTime = book.getLastModifiedTime();

			// FIXME
			lastSync = localTime;
			
			state = null;
		}
		
		public void setRemote(UUID uuid, String title, Time mtime) {
			remote = true;
			Assert.assertTrue(this.uuid == null || this.uuid.equals(uuid));
			this.uuid = uuid;
			remoteTitle = title;
			remoteTime = mtime;
			state = null;
		}
		
		private State computeState() {
			Assert.assertTrue(local || remote);
			if (!local)
				return State.REMOTE_IS_NEWER;
			if (!remote) 
				return State.LOCAL_ONLY;
			if (localTime.equals(remoteTime))
				return State.IN_SYNC;
			if (lastSync == null)
				return State.CONFLICT;
			boolean localIsUnchanged = localTime.equals(lastSync);
			boolean remoteIsUnchanged = remoteTime.equals(lastSync);
			if (localTime.before(remoteTime) && localIsUnchanged)
				return State.REMOTE_IS_NEWER;
			if (localTime.after(remoteTime) && remoteIsUnchanged)
				return State.LOCAL_IS_NEWER;
			return State.CONFLICT;
		}
				
		public UUID getUuid() {
			return uuid;
		}
		
		public State getState() {
			if (state == null)
				state = computeState();
			return state;
		}
		
		public Time getLastModTime() {
			Assert.assertTrue(local || remote);
			if (!local)
				return remoteTime;
			if (!remote) 
				return localTime;
			if (localTime.after(remoteTime))
				return localTime;
			else
				return remoteTime;
		}
		
		public String getTitle() {
			switch (getState()) {
			case CONFLICT: 
			case LOCAL_IS_NEWER:
			case LOCAL_ONLY:
			case IN_SYNC:
				return localTitle;
			case REMOTE_IS_NEWER:
				return remoteTitle;
			}
			Assert.fail(); // unreachable
			return null;
		}
		
		public String toString() {
			return getTitle();
		}
	}
	
	protected LinkedList<SyncItem> data = new LinkedList<SyncItem>();
	
	public static class SyncItemComparator implements Comparator<SyncItem> {
		@Override
		public int compare(SyncItem lhs, SyncItem rhs) {
			SyncItem.State lhsState = lhs.getState();
			SyncItem.State rhsState = rhs.getState();
			// Conflict is on top
			if ((!lhsState.equals(State.CONFLICT)) && ( rhsState.equals(State.CONFLICT))) return +1;
			if (( lhsState.equals(State.CONFLICT)) && (!rhsState.equals(State.CONFLICT))) return -1;

			// Local-only is next to bottom
			if (( lhsState.equals(State.LOCAL_ONLY)) && (!rhsState.equals(State.LOCAL_ONLY))) return +1;
			if ((!lhsState.equals(State.LOCAL_ONLY)) && ( rhsState.equals(State.LOCAL_ONLY))) return -1;

			// in sync is at bottom
			if (( lhsState.equals(State.IN_SYNC)) && (!rhsState.equals(State.IN_SYNC))) return +1;
			if ((!lhsState.equals(State.IN_SYNC)) && ( rhsState.equals(State.IN_SYNC))) return -1;

			// otherwise sort by time			
			Time lhsTime = lhs.getLastModTime();
			Time rhsTime = rhs.getLastModTime();
			if (lhsTime.before(rhsTime)) return +1;
			if (lhsTime.after(rhsTime)) return -1;
			
			// if all else fails, sort by uuid
			return lhs.getUuid().toString().compareToIgnoreCase(rhs.getUuid().toString());
		}
	}
	
	protected void addLocal(BookPreview book) {
		UUID uuid = book.getUUID();
		Log.e(TAG, "addLocal "+uuid);
		for (SyncItem item : data)
			if (item.getUuid().equals(uuid)) {
				item.setLocal(book);
				return;
			}
		SyncItem item = new SyncItem();
		item.setLocal(book);
		data.add(item);
	}
	
	protected void addRemote(UUID uuid, String title, Time mtime) {
		for (SyncItem item : data)
			if (item.getUuid().equals(uuid)) {
				item.setRemote(uuid, title, mtime);
				return;
			}
		SyncItem item = new SyncItem();
		item.setRemote(uuid, title, mtime);
		data.add(item);
	}
	
	protected void sort() {
		Collections.sort(data, new SyncItemComparator());
	}
	
	public void reset() {
		data.clear();
		addLocalBookshelf();
		
		addRemote(UUID.fromString("9f9ec0eb-d3e1-40c3-b6b8-e28dbd951008"), "Title", new Time());
		sort();
	}
	
	private void addLocalBookshelf() {
		for (BookPreview book : Bookshelf.getBookPreviewList())
			addLocal(book);
	}

	protected SyncItem get(int i) {
		return data.get(i);
	}
}
